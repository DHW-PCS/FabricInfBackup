package io.github.initauther97.backup;

import net.minecraft.util.Pair;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ChunkBackupStorage {
    public final Path root;
    public ChunkBackupStorage(String path) {
        this.root = Path.of(path);
        try {
            Files.createDirectory(root);
        } catch (IOException e) {
            if(e instanceof FileAlreadyExistsException) {}
            else throw new RuntimeException(e);
        }
    }

    private final List<Pair<Path, BackupInfo>> allBackups = new ArrayList<>();

    public void init() throws IOException {
        var visitor = new FileVisitor<Path>() {
            int depth = 0;
            Path pth;
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if(depth ++ < 2) {
                    pth = dir;
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if(file.getFileName().toString().equals(ChunkBackup.BACKUP_INFO.getFileName().toString())) {
                    try {
                        BackupInfo info = BackupInfo.deserialize(Files.readAllLines(file));
                        allBackups.add(new Pair<>(pth, info));
                    } catch (Exception e) {
                        Backup.LOGGER.error("Detected wrong format cfg file for backup {}, ignoring", pth);
                        return FileVisitResult.CONTINUE;
                    }
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
        };
        Files.walkFileTree(root, visitor);
        allBackups.sort(Backup.COMPARATOR);
    }

    public Pair<Path, BackupInfo> find(int id) {
        if(id >= allBackups.size()) {
            return null;
        }
        return allBackups.get(id);
    }

    public void forEachIndexed(BiConsumer<Integer, Pair<Path, BackupInfo>> consumer) {
        int size = allBackups.size();
        for(int i = 0; i < size; i++) {
            consumer.accept(i, allBackups.get(i));
        }
    }

    public int size() {
        return allBackups.size();
    }

    public ChunkBackup createChunkBackup(String desc) {
        return new ChunkBackup(this, desc);
    }

    protected Path create(Path workPath, ChunkBackup backup) throws IOException {
        Backup.LOGGER.info("Backing up the region files");
        Path bkRoot = root.resolve("BACKUP_"+System.currentTimeMillis());
        Files.createDirectories(bkRoot);
        Set<String> another = backup.affectedRegions.stream().map(RegionPos::getFileName).collect(Collectors.toSet());
        Path pth = workPath.resolve(Backup.REGION_PATH);
        Backup.LOGGER.info("Walking through:" + pth);
        Files.walkFileTree(pth, new FileVisitor<>() {
            int depth = 0;
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Backup.LOGGER.info("Pre-visit directory:"+dir);
                if(depth ++ < 1) {
                    Backup.LOGGER.info("Continue!");
                    return FileVisitResult.CONTINUE;
                } else return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Backup.LOGGER.info("Visit file: "+file);
                Path name = file.getFileName();
                if(!another.removeIf(it -> it.equals(name.toString()))) {
                    return FileVisitResult.CONTINUE;
                }
                Files.copy(file, bkRoot.resolve(name));
                Backup.LOGGER.info("Copied {}", file.getFileName().toString());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                depth--;
                return FileVisitResult.CONTINUE;
            }
        });
        BackupInfo info = new BackupInfo(backup.begin, backup.end, new Date(), backup.description);
        Path bkInfo = bkRoot.resolve(ChunkBackup.BACKUP_INFO.getFileName());
        Files.write(bkInfo, info.serialize());
        allBackups.add(new Pair<>(bkRoot, info));
        allBackups.sort(Backup.COMPARATOR);
        Backup.LOGGER.info("Backup finished");
        return bkRoot;
    }
}
