package org.dhwpcs.inf_backup.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Pair;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.dimension.DimensionType;
import org.dhwpcs.inf_backup.FabricEntrypoint;
import org.dhwpcs.inf_backup.storage.Backup;
import org.dhwpcs.inf_backup.storage.BackupInfo;
import org.dhwpcs.inf_backup.storage.BackupStorage;
import org.dhwpcs.inf_backup.storage.RegionMerger;

import java.nio.file.Path;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;

public class ServerStoppedHandler implements ServerLifecycleEvents.ServerStopped {
    private final FabricEntrypoint entrypoint;

    public ServerStoppedHandler(FabricEntrypoint entrypoint) {

        this.entrypoint = entrypoint;
    }

    @Override
    public void onServerStopped(MinecraftServer server) {
        boolean errored = false;
        RuntimeException re = new RuntimeException("Exception restoring chunks") {
            @Override
            public Throwable fillInStackTrace() {
                setStackTrace(Backup.EMPTY_STACK_TRACE);
                return this;
            }
        };
        SortedSet<Pair<Path, BackupInfo>> infos = entrypoint.selectedBackups;

        for (Pair<Path, BackupInfo> info : infos) {
            Backup.LOGGER.info("Now begin to rollback chunks");
            BackupInfo right = info.getRight();
            Path left = info.getLeft();
            Path root = server.getSavePath(WorldSavePath.ROOT);
            root = DimensionType.getSaveDirectory(RegistryKey.of(RegistryKeys.WORLD, right.dim()), root);
            try (
                    RegionMerger region = new RegionMerger(
                            left.resolve(Backup.REGION_PATH),
                            root.resolve(Backup.REGION_PATH),
                            right.begin(),
                            right.end()
                    );
                    RegionMerger entities = new RegionMerger(
                            left.resolve(Backup.ENTITIES_PATH),
                            root.resolve(Backup.ENTITIES_PATH),
                            right.begin(),
                            right.end()
                    );
                    BackupStorage storage = entrypoint.storage
            ) {
                storage.backupRestoration(info);
                try {
                    CompletableFuture.allOf(region.merge(), entities.merge()).join();
                    Backup.LOGGER.info("The restoration is done. Please restart the server.");
                } catch (RuntimeException e) {
                    errored = true;
                    Backup.LOGGER.fatal("FAILED TO ROLLBACK CHUNKS!", e);
                    Backup.LOGGER.fatal("The modifies cannot be restored. Please refer to " + left + " and replace the ones in /region with them.");
                    Backup.LOGGER.fatal("Current rollback failed. However, you can still roll back to the same save when you know where the problem came.");
                    re.addSuppressed(new RuntimeException("failed to restore chunk in backup " + right, e));
                }
            } catch (Throwable e) {
                errored = true;
                re.addSuppressed(new RuntimeException("failed to restore chunk in backup " + right, e));
            } finally {
                infos.clear();
            }
        }
        if (errored) {
            re.printStackTrace();
        }
    }
}
