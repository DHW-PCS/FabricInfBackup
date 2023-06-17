package io.github.initauther97.backup;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.storage.RegionBasedStorage;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

public class ChunkMerger {

    private static final Constructor<RegionBasedStorage> RBS_NEW;
    private static final Method RBS_WRITE;

    static {
        try {
            RBS_NEW = RegionBasedStorage.class.getDeclaredConstructor(Path.class, Boolean.TYPE);
            RBS_NEW.setAccessible(true);
            RBS_WRITE = RegionBasedStorage.class.getDeclaredMethod("write", ChunkPos.class, NbtCompound.class);
            RBS_WRITE.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final RegionBasedStorage sFrom;
    private final RegionBasedStorage sTo;
    private final Path to;
    private final String description;
    private final ChunkPos begin;
    private final ChunkPos end;
    private final Set<RegionPos> affectedRegions = new HashSet<>();

    public ChunkMerger(Path from, Path to, String description, ChunkPos begin, ChunkPos end) throws Throwable {
        sFrom = RBS_NEW.newInstance(from, true);
        sTo = RBS_NEW.newInstance(to, true);
        this.to = to;
        this.description = description;
        this.begin = begin;
        this.end = end;
        ChunkPos.stream(begin, end).forEach(it -> affectedRegions.add(new RegionPos(it.getRegionX(), it.getRegionZ())));
    }

    public void merge() {
        ChunkPos.stream(begin, end).forEach(pos -> {
            try {
                NbtCompound compound = sFrom.getTagAt(pos);
                RBS_WRITE.invoke(sTo, pos, compound);
                Backup.LOGGER.info("Merged chunk {}", pos.toString());
            } catch (Throwable t) {
                throw new RuntimeException("Failed to merge chunk " + pos.toString(), t);
            }
        });
    }

    public void createBackup(Path backupRoot) throws IOException {
        Set<String> set = new HashSet<>();
        affectedRegions.forEach(it -> set.add(it.getFileName()));
        Backup.LOGGER.info("Backing up the region files to be modified");
        Path bkRoot = backupRoot.resolve("BACKUP_"+ description +"_"+System.currentTimeMillis());
        Files.createDirectories(bkRoot);
        Files.walkFileTree(to, new FileVisitor<>() {
            int depth = 0;
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if(depth ++ < 1) {
                    return FileVisitResult.CONTINUE;
                } else return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(set.contains(file.getFileName().toString())) {
                    Files.copy(file, bkRoot.resolve(file.getFileName()));
                    Backup.LOGGER.info("Copied {}", file.getFileName().toString());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        Backup.LOGGER.info("Backup finished");
    }
}
