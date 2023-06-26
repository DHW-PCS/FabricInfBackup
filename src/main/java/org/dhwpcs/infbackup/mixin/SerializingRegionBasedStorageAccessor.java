package org.dhwpcs.infbackup.mixin;

import net.minecraft.world.storage.SerializingRegionBasedStorage;
import net.minecraft.world.storage.StorageIoWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SerializingRegionBasedStorage.class)
public interface SerializingRegionBasedStorageAccessor {
    @Accessor
    StorageIoWorker getWorker();
}
