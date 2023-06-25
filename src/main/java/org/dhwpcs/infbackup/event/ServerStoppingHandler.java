package org.dhwpcs.infbackup.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import org.dhwpcs.infbackup.FabricEntrypoint;
import org.dhwpcs.infbackup.storage.Backup;
import org.dhwpcs.infbackup.storage.BackupInfo;

import java.io.IOException;
import java.nio.file.Files;
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
        if(!Files.exists(entrypoint.config_file)) {
            try {
                Files.createFile(entrypoint.config_file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            Files.write(entrypoint.config_file, entrypoint.gson.toJson(entrypoint.config).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
