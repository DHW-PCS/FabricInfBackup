package org.dhwpcs.infbackup.mixin;

import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.world.storage.ChunkDataAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.swing.text.html.parser.Entity;

@Mixin(ServerEntityManager.class)
public interface ServerEntityManagerAccessor {
    @Accessor
    ChunkDataAccess<Entity> getDataAccess();
}
