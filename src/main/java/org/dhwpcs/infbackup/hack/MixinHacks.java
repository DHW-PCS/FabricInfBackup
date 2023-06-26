package org.dhwpcs.infbackup.hack;

import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.storage.StorageIoWorker;
import org.dhwpcs.infbackup.mixin.EntityChunkDataAccessAccessor;
import org.dhwpcs.infbackup.mixin.ServerEntityManagerAccessor;
import org.dhwpcs.infbackup.mixin.ServerWorldAccessor;
import org.dhwpcs.infbackup.mixin.SerializingRegionBasedStorageAccessor;

import java.util.Collection;

public class MixinHacks {
    public static StorageIoWorker getEntityStorage(ServerWorld sw) {
        ServerWorldAccessor swA = (ServerWorldAccessor) sw;
        ServerEntityManagerAccessor semA = (ServerEntityManagerAccessor) swA.getEntityManager();
        EntityChunkDataAccessAccessor scdaA = (EntityChunkDataAccessAccessor) semA.getDataAccess();
        return scdaA.getDataLoadWorker();
    }

    public static StorageIoWorker getChunkStorage(ServerWorld sw) {
        ServerChunkManager scm = sw.getChunkManager();
        ThreadedAnvilChunkStorage tacs =  scm.threadedAnvilChunkStorage;
        return (StorageIoWorker) tacs.getWorker();
    }

    public static StorageIoWorker getPoiStorage(ServerWorld sw) {
        SerializingRegionBasedStorageAccessor srbsA = (SerializingRegionBasedStorageAccessor) sw.getPointOfInterestStorage();
        return srbsA.getWorker();
    }

    public static void reloadPoi(ServerWorld sw, Collection<ChunkPos> all) {
        SerializingRegionBasedStorageAccessor srbsA = (SerializingRegionBasedStorageAccessor) sw.getPointOfInterestStorage();
        all.forEach(srbsA::loadFromWorker);
    }
}
