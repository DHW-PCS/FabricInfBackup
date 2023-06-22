package org.dhwpcs.inf_backup.storage;

import org.dhwpcs.inf_backup.util.RegionPos;
import org.dhwpcs.inf_backup.util.Util;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class ChunkBackup {

    final Identifier dim;
    final String description;
    ChunkPos begin;
    ChunkPos end;
    Set<RegionPos> affectedRegions = new HashSet<>();
    final BackupStorage storageIn;

    public static final Path BACKUP_INFO = Path.of("backup_info.txt");

    public ChunkBackup(BackupStorage storageIn, Identifier dim, String description) {
        this.storageIn = storageIn;
        this.dim = dim;
        this.description = description;
    }
    public void addRegionIn(ChunkPos begin, ChunkPos end) {
        ChunkPos.stream(begin, end).forEach(it -> affectedRegions.add(new RegionPos(it.getRegionX(), it.getRegionZ())));
        this.begin = begin;
        this.end = end;
    }

    public void backup(ServerWorld worldIn) throws IOException {
        List<UUID> entities = new LinkedList<>();
        worldIn.iterateEntities().forEach(it -> {
            if(Util.within(it.getBlockPos(), begin, end)) {
                entities.add(it.getUuid());
            }
        });
        storageIn.create(this, entities);
    }
}
