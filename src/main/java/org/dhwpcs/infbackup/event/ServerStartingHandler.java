package org.dhwpcs.infbackup.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.dhwpcs.infbackup.FabricEntrypoint;
import org.dhwpcs.infbackup.config.InfBackupConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerStartingHandler implements ServerLifecycleEvents.ServerStarting {

    private final FabricEntrypoint entrypoint;

    public ServerStartingHandler(FabricEntrypoint entrypoint) {
        this.entrypoint = entrypoint;
    }

    @Override
    public void onServerStarting(MinecraftServer server) {
        Path configDir = entrypoint.config_file;
        if(!Files.exists(configDir)) {
            entrypoint.config = InfBackupConfig.getDefault();
        } else {
            String content;
            try {
                content = Files.readString(configDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            entrypoint.config = entrypoint.gson.fromJson(content, InfBackupConfig.class);
        }
    }
}
