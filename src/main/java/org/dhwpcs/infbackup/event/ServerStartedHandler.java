package org.dhwpcs.infbackup.event;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.WorldSavePath;
import org.dhwpcs.infbackup.FabricEntrypoint;
import org.dhwpcs.infbackup.storage.BackupStorage;

import java.io.IOException;
import java.nio.file.Path;

public class ServerStartedHandler implements ServerLifecycleEvents.ServerStarted {

    private final FabricEntrypoint entrypoint;

    public ServerStartedHandler(FabricEntrypoint entrypoint) {
        this.entrypoint = entrypoint;
    }
    @Override
    public void onServerStarted(MinecraftServer server) {
        Path saveRoot = server.getSavePath(WorldSavePath.ROOT);
        BackupStorage storage;
        entrypoint.storage = storage = new BackupStorage(saveRoot.resolve("backup_storage"), saveRoot);
        try {
            storage.init();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
        entrypoint.rootCmd.register(dispatcher);
    }
}
