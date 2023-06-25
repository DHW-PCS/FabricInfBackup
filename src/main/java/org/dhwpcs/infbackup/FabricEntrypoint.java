package org.dhwpcs.infbackup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Pair;
import org.dhwpcs.infbackup.command.CommandRoot;
import org.dhwpcs.infbackup.config.InfBackupConfig;
import org.dhwpcs.infbackup.config.StringSerializable;
import org.dhwpcs.infbackup.config.StringSerializableSerializer;
import org.dhwpcs.infbackup.event.*;
import org.dhwpcs.infbackup.storage.Backup;
import org.dhwpcs.infbackup.storage.BackupInfo;
import org.dhwpcs.infbackup.storage.BackupStorage;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class FabricEntrypoint implements ModInitializer {

    public BackupStorage storage;
    public SortedSet<Pair<Path, BackupInfo>> selectedBackups = new TreeSet<>(Backup.COMPARATOR);
    public Map<String, LastOperation> operationToConfirm = new HashMap<>();

    public Path config_file = FabricLoader.getInstance().getConfigDir().resolve("inf_backup.json");

    public Gson gson = new GsonBuilder().registerTypeAdapter(StringSerializable.class, new StringSerializableSerializer()).setPrettyPrinting().create();

    public InfBackupConfig config;

    public CommandRoot rootCmd = new CommandRoot(this);

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(new ServerStartingHandler(this));
        ServerLifecycleEvents.SERVER_STARTED.register(new ServerStartedHandler(this));
        ServerTickEvents.START_SERVER_TICK.register(new StartServerTickHandler(this));
        ServerLifecycleEvents.SERVER_STOPPING.register(new ServerStoppingHandler(this));
        ServerLifecycleEvents.SERVER_STOPPED.register(new ServerStoppedHandler(this));
    }
}
