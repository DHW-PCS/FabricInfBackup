package io.github.initauther97.backup;

import net.minecraft.util.math.ChunkPos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ChunkBackup {

    final String description;
    ChunkPos begin;
    ChunkPos end;
    Set<RegionPos> affectedRegions = new HashSet<>();
    final ChunkBackupStorage storageIn;

    public static final Path BACKUP_INFO = Path.of("backup_info.txt");

    public ChunkBackup(ChunkBackupStorage storageIn, String description) {
        this.storageIn = storageIn;
        this.description = description;
    }
    public void addRegionIn(ChunkPos begin, ChunkPos end) {
        ChunkPos.stream(begin, end).forEach(it -> affectedRegions.add(new RegionPos(it.getRegionX(), it.getRegionZ())));
        this.begin = begin;
        this.end = end;
    }

    public Path backup(Path workPath) throws IOException {
        return storageIn.create(workPath,this);
    }
}
