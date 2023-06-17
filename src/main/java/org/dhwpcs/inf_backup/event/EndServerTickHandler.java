package org.dhwpcs.inf_backup.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.dhwpcs.inf_backup.FabricEntrypoint;

public class EndServerTickHandler implements ServerTickEvents.EndTick {
    private final FabricEntrypoint entrypoint;

    public EndServerTickHandler(FabricEntrypoint entrypoint) {
        this.entrypoint = entrypoint;
    }

    @Override
    public void onEndTick(MinecraftServer server) {

    }
}
