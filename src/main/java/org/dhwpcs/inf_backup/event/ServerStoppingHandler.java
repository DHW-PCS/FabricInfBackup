package org.dhwpcs.inf_backup.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import org.dhwpcs.inf_backup.FabricEntrypoint;
import org.dhwpcs.inf_backup.storage.Backup;
import org.dhwpcs.inf_backup.storage.BackupInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServerStoppingHandler implements ServerLifecycleEvents.ServerStopping {
    private final FabricEntrypoint entrypoint;

    public ServerStoppingHandler(FabricEntrypoint entrypoint) {

        this.entrypoint = entrypoint;
    }


    @Override
    public void onServerStopping(MinecraftServer server) {
        entrypoint.operationToConfirm.clear();
        for (Pair<Path, BackupInfo> info : entrypoint.selectedBackups) {
            BackupInfo bi = info.getRight();
            List<UUID> entities = new ArrayList<>(bi.entities());
            ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, bi.dim()));
            if (world == null) {
                Backup.LOGGER.error("Backup {} is in world {} that is unsupported!", bi.uid(), bi.dim());
                continue;
            }
            entities.forEach(uid -> {
                Entity e = world.getEntity(uid);
                if (e != null) {
                    e.setRemoved(Entity.RemovalReason.KILLED);
                }
            });
        }
    }
}
