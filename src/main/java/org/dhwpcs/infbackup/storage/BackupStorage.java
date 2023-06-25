package org.dhwpcs.infbackup.storage;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.world.dimension.DimensionType;
import org.dhwpcs.infbackup.util.RegionPos;
import org.dhwpcs.infbackup.util.Util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
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
                Backup.LOGGER.debug("Stepping into {}", dir);
                Backup.LOGGER.debug("Depth is {}", depth);
                if (depth < 2) {
                    depth++;
                    pth = dir;
                    Backup.LOGGER.debug("Enter.");
                    return FileVisitResult.CONTINUE;
                }
                Backup.LOGGER.debug("Skip.");
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Backup.LOGGER.debug("Visit: {}", file);
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
                Backup.LOGGER.debug("Stepping out of {}", dir);
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
            Files.walkFileTree(pair.getLeft(), new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    exc.printStackTrace();
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
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
        Files.createDirectories(regionRoot);
        Files.createDirectories(entitiesRoot);
        Set<String> another = backup.affectedRegions.stream().map(RegionPos::getFileName).collect(Collectors.toSet());
        Path regionPth = resolveDim(backup.dim, Backup.REGION_PATH);
        Path entitiesPth = resolveDim(backup.dim, Backup.ENTITIES_PATH);
        Function<Path, FileVisitor<Path>> copier = path -> new FileVisitor<>() {
            int depth = 0;

            {
                Backup.LOGGER.debug("Walking through:{}", path);
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Backup.LOGGER.debug("Stepping into {}", dir);
                Backup.LOGGER.debug("Depth is {}", depth);
                if (depth < 1) {
                    depth++;
                    Backup.LOGGER.debug("Enter.");
                    return FileVisitResult.CONTINUE;
                } else {
                    Backup.LOGGER.debug("Skip.");
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path name = file.getFileName();
                Backup.LOGGER.debug("Visit: {}", file);
                if (!another.contains(name.toString())) {
                    return FileVisitResult.CONTINUE;
                }
                Files.copy(file, path.resolve(name));
                Backup.LOGGER.info("Copied {}", file.getFileName().toString());
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
                Backup.LOGGER.debug("Stepping out of {}", dir);
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(regionPth, copier.apply(regionRoot));
        Files.walkFileTree(entitiesPth, copier.apply(entitiesRoot));
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
                Backup.LOGGER.debug("Stepping into {}", dir);
                Backup.LOGGER.debug("Depth is {}", depth);
                if (depth < 1) {
                    depth++;
                    Backup.LOGGER.debug("Enter.");
                    return FileVisitResult.CONTINUE;
                } else {
                    Backup.LOGGER.debug("Skip.");
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Backup.LOGGER.debug("Visit: {}", file);
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
                Backup.LOGGER.debug("Stepping out of {}", dir);
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(region, visitor.apply(getBackupRegion(bkRoot)));
        Files.walkFileTree(entities, visitor.apply(getBackupEntities(bkRoot)));
        Backup.LOGGER.info("Backup finished");
    }

    private Path resolveDim(Identifier dimension, Path resolve) {
        return DimensionType.getSaveDirectory(RegistryKey.of(RegistryKeys.WORLD, dimension), worldRoot).resolve(resolve);
    }

    private Path getBackupRegion(Path bkRoot) {
        return bkRoot.resolve(Backup.REGION_PATH);
    }

    private Path getBackupEntities(Path bkRoot) {
        return bkRoot.resolve(Backup.ENTITIES_PATH);
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
