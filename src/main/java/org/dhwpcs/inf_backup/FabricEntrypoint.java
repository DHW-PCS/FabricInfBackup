package org.dhwpcs.inf_backup;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Pair;
import org.dhwpcs.inf_backup.command.CommandRoot;
import org.dhwpcs.inf_backup.event.EndServerTickHandler;
import org.dhwpcs.inf_backup.event.ServerStartedHandler;
import org.dhwpcs.inf_backup.event.ServerStoppedHandler;
import org.dhwpcs.inf_backup.event.ServerStoppingHandler;
import org.dhwpcs.inf_backup.storage.Backup;
import org.dhwpcs.inf_backup.storage.BackupInfo;
import org.dhwpcs.inf_backup.storage.BackupStorage;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class FabricEntrypoint implements ModInitializer {

    public BackupStorage storage;
    public SortedSet<Pair<Path, BackupInfo>> selectedBackups = new TreeSet<>(Backup.COMPARATOR);
    public Map<String, LastOperation> operationToConfirm = new HashMap<>();

    public CommandRoot rootCmd = new CommandRoot(this);

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(new ServerStartedHandler(this));
        ServerTickEvents.END_SERVER_TICK.register(new EndServerTickHandler(this));
        ServerLifecycleEvents.SERVER_STOPPING.register(new ServerStoppingHandler(this));
        ServerLifecycleEvents.SERVER_STOPPED.register(new ServerStoppedHandler(this));
    }
}
