package org.dhwpcs.infbackup.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.dhwpcs.infbackup.FabricEntrypoint;

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
        Path configDir = FabricLoader.getInstance().getConfigDir();
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Path file = configDir.resolve(entrypoint.config_file);
        if(!Files.exists(file)) {

        }
    }
}
