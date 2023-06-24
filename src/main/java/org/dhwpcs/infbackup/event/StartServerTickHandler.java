package org.dhwpcs.infbackup.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.dhwpcs.infbackup.FabricEntrypoint;

public class StartServerTickHandler implements ServerTickEvents.StartTick {
    private final FabricEntrypoint entrypoint;

    public StartServerTickHandler(FabricEntrypoint entrypoint) {
        this.entrypoint = entrypoint;
    }

    @Override
    public void onStartTick(MinecraftServer server) {

    }
}
