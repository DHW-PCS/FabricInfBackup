package org.dhwpcs.infbackup.storage;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.Level;
import org.dhwpcs.infbackup.hack.MixinHacks;
import org.dhwpcs.infbackup.util.RegionPos;
import org.dhwpcs.infbackup.util.Util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BackupStorage //implements Closeable
{
    public final Path storage;
    private final Path worldRoot;

    public BackupStorage(Path storage, Path worldRoot) {
        this.storage = storage;
        this.worldRoot = worldRoot;
        try {
            Files.createDirectories(this.storage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final List<Pair<Path, BackupInfo>> allBackups = new ArrayList<>();

    public void init() throws IOException {
        var visitor = new FileVisitor<Path>() {
            int depth = 0;
            Path pth;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (depth < 2) {
                    depth++;
                    pth = dir;
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (depth == 2 && file.getFileName().toString().equals(ChunkBackup.BACKUP_INFO.getFileName().toString())) {
                    try {
                        Backup.LOGGER.info("Loading config file {}", file);
                        BackupInfo info = BackupInfo.deserialize(Files.readAllLines(file));
                        allBackups.add(new Pair<>(pth, info));
                    } catch (Exception e) {
                        Backup.LOGGER.error("Detected wrong format config file for {}, ignoring", pth);
                        return FileVisitResult.CONTINUE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                Backup.LOGGER.error("Walking failed with exception", exc);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                --depth;
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(storage, visitor);
        allBackups.sort(Backup.COMPARATOR);
    }

    public Pair<Path, BackupInfo> find(int id) {
        if (id >= allBackups.size()) {
            return null;
        }
        return allBackups.get(id);
    }

    public boolean delete(int id) throws IOException {
        assert id >= 0;
        if (id < allBackups.size()) {
            Pair<Path, BackupInfo> pair = allBackups.get(id);
            Files.walkFileTree(pair.getLeft(), Util.deleteDirectory());
            allBackups.remove(id);
            return true;
        } else return false;
    }

    public void forEachIndexed(BiConsumer<Integer, Pair<Path, BackupInfo>> consumer) {
        Util.forEachIndexed(allBackups, consumer);
    }

    public int size() {
        return allBackups.size();
    }

    public ChunkBackup createChunkBackup(Identifier dim, String desc) {
        return new ChunkBackup(this, dim, desc);
    }

    protected void create(ChunkBackup backup, List<UUID> entities) throws IOException {
        Backup.LOGGER.info("Backing up the region files");
        Date now = new Date();
        Path bkRoot = storage.resolve("BACKUP_" + Backup.FORMATTER.format(now));
        Path regionRoot = getBackupRegion(bkRoot);
        Path entitiesRoot = getBackupEntities(bkRoot);
        Path poiRoot = getBackupPoi(bkRoot);
        Files.createDirectories(regionRoot);
        Files.createDirectories(entitiesRoot);
        Set<String> another = backup.affectedRegions.stream().map(RegionPos::getFileName).collect(Collectors.toSet());
        Path regionPth = resolveDim(backup.dim, Backup.REGION_PATH);
        Path entitiesPth = resolveDim(backup.dim, Backup.ENTITIES_PATH);
        Path poiPth = resolveDim(backup.dim, Backup.POI_PATH);
        Function<Path, FileVisitor<Path>> copier = path -> new FileVisitor<>() {
            int depth = 0;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (depth < 1) {
                    depth++;
                    return FileVisitResult.CONTINUE;
                } else {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path name = file.getFileName();
                if (!another.contains(name.toString())) {
                    return FileVisitResult.CONTINUE;
                }
                Files.copy(file, path.resolve(name));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                Backup.LOGGER.error("Walking failed with exception", exc);
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                --depth;
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(regionPth, copier.apply(regionRoot));
        Files.walkFileTree(entitiesPth, copier.apply(entitiesRoot));
        Files.walkFileTree(poiPth, copier.apply(poiRoot));
        BackupInfo info = new BackupInfo(newUUID(), backup.dim, backup.begin, backup.end, now, backup.description, entities);
        Path bkInfo = bkRoot.resolve(ChunkBackup.BACKUP_INFO.getFileName());
        Files.write(bkInfo, info.serialize());
        Pair<Path, BackupInfo> result = new Pair<>(bkRoot, info);
        allBackups.add(result);
        allBackups.sort(Backup.COMPARATOR);
        Backup.LOGGER.info("Backup finished");
    }

    public void backupRestoration(Pair<Path, BackupInfo> pair) throws IOException {
        Backup.LOGGER.info("Backing up the region files to be modified");
        BackupInfo info = pair.getRight();
        Date now = new Date();
        Set<String> set = RegionPos
                .betweenClosed(info.begin(), info.end())
                .map(RegionPos::getFileName)
                .collect(Collectors.toSet());
        Path bkRoot = pair.getLeft().resolve("RESTORATION_" + Backup.FORMATTER.format(now));
        Files.createDirectories(bkRoot);
        Path region = resolveDim(info.dim(), Backup.REGION_PATH);
        Path entities = resolveDim(info.dim(), Backup.ENTITIES_PATH);
        Path poi = resolveDim(info.dim(), Backup.POI_PATH);
        Function<Path, FileVisitor<Path>> visitor = path -> new FileVisitor<>() {
            int depth = 0;

            {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (depth < 1) {
                    depth++;
                    return FileVisitResult.CONTINUE;
                } else {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (set.contains(file.getFileName().toString())) {
                    Files.copy(file, path.resolve(file.getFileName()));
                    Backup.LOGGER.info("Copied {}", file.getFileName().toString());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                Backup.LOGGER.error("Walking failed with exception", exc);
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                depth--;
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(region, visitor.apply(getBackupRegion(bkRoot)));
        Files.walkFileTree(entities, visitor.apply(getBackupEntities(bkRoot)));
        Files.walkFileTree(poi, visitor.apply(getBackupPoi(bkRoot)));
        Backup.LOGGER.info("Backup finished");
    }

    public boolean doMergeDynamic(
            int id,
            MinecraftServer server,
            BiConsumer<Level, String> printer
    ) {
        Pair<Path, BackupInfo> pair = find(id);
        BackupInfo right = pair.getRight();
        ServerWorld sw = server.getWorld(RegistryKey.of(Registry.WORLD_KEY, right.dim()));
        if (sw == null) {
            printer.accept(Level.ERROR, String.format("Backup %s is in world %s that is unsupported!", right.uid(), right.dim()));
            return false;
        }
        List<ChunkPos> chunkToUnload = ChunkPos.stream(right.begin(), right.end()).filter(it -> sw.isChunkLoaded(it.x, it.z)).toList();
        if(!chunkToUnload.isEmpty()) {
            printer.accept(Level.ERROR, "These chunks are still loaded: "+chunkToUnload);
            return false;
        }
        right.entities().forEach(uid -> {
            Entity e = sw.getEntity(uid);
            if (e != null) {
                e.setRemoved(Entity.RemovalReason.KILLED);
            }
        });
        server.saveAll(false, true, true);
        Path left = pair.getLeft();
        try (
                RegionMerger region = new RegionMerger(
                        left.resolve(Backup.REGION_PATH),
                        MixinHacks.getChunkStorage(sw),
                        right.begin(),
                        right.end()
                );
                RegionMerger entities = new RegionMerger(
                        left.resolve(Backup.ENTITIES_PATH),
                        MixinHacks.getEntityStorage(sw),
                        right.begin(),
                        right.end()
                );
                RegionMerger poi = new RegionMerger(
                        left.resolve(Backup.POI_PATH),
                        MixinHacks.getPoiStorage(sw),
                        right.begin(),
                        right.end()
                )
        ) {
            backupRestoration(pair);
            try {
                CompletableFuture.allOf(region.merge(), entities.merge(), poi.merge()).join();
                printer.accept(Level.INFO, "The restoration is done.");
                return true;
            } catch (RuntimeException e) {
                printer.accept(Level.FATAL, "FAILED TO ROLLBACK CHUNKS!");
                printer.accept(Level.FATAL, "The modifies cannot be restored. Please refer to " + left + " and replace the ones in /region with them.");
                printer.accept(Level.ERROR, "Current rollback failed. However, you can still roll back to the same save when you know where the problem came.");
                return false;
            }
        } catch (Throwable e) {
            printer.accept(Level.ERROR, "We cannot roll back at this moment. Backing up of current file failed.");
            return false;
        }
    }

    private Path resolveDim(Identifier dimension, Path resolve) {
        return DimensionType.getSaveDirectory(RegistryKey.of(Registry.WORLD_KEY, dimension), worldRoot).resolve(resolve);
    }

    private static Path getBackupRegion(Path bkRoot) {
        return bkRoot.resolve(Backup.REGION_PATH);
    }

    private static Path getBackupEntities(Path bkRoot) {
        return bkRoot.resolve(Backup.ENTITIES_PATH);
    }

    private static Path getBackupPoi(Path bkRoot) {
        return bkRoot.resolve(Backup.POI_PATH);
    }
    private UUID newUUID() {
        UUID result;
        while (true) {
            result = UUID.randomUUID();
            for (Pair<Path, BackupInfo> pair : allBackups) {
                if (pair.getRight().uid().equals(result)) {
                    result = null;
                }
            }
            if (result != null)
                return result;
        }
    }
}
