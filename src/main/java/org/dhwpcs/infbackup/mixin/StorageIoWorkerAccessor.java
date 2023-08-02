package org.dhwpcs.infbackup.mixin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.storage.StorageIoWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
@Mixin(StorageIoWorker.class)
public interface StorageIoWorkerAccessor {
    @Invoker("<init>")
    static StorageIoWorker create(Path directory, boolean dsync, String name) {
        throw new AbstractMethodError();
    }

    @Invoker("readChunkData")
    CompletableFuture<NbtCompound> fetchChunkData(ChunkPos pos);
}
