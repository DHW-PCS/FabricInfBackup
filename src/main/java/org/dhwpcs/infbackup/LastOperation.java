package org.dhwpcs.infbackup;

import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;
import org.dhwpcs.infbackup.storage.Backup;
import org.dhwpcs.infbackup.storage.BackupInfo;
import org.dhwpcs.infbackup.storage.BackupStorage;
import org.dhwpcs.infbackup.storage.RegionMerger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LastOperation {
    boolean perform(ServerCommandSource source);

    String getInformation();

    class DeleteSave implements LastOperation {
        private final int id;
        private final BackupStorage storage;

        public DeleteSave(int id, BackupStorage storage) {
            this.id = id;
            this.storage = storage;
        }

        @Override
        public boolean perform(ServerCommandSource source) {
            try {
                return storage.delete(id);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public String getInformation() {
            return "DeleteSave " + id;
        }
    }

    class RollbackDirect implements LastOperation {
        private final int id;
        private final BackupStorage storage;

        public RollbackDirect(int id, BackupStorage storage) {
            this.id = id;
            this.storage = storage;
        }

        @Override
        public boolean perform(ServerCommandSource source) {
            MinecraftServer server = source.getServer();
            Pair<Path, BackupInfo> pair = storage.find(id);
            BackupInfo right = pair.getRight();
            ServerWorld sw = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, right.dim()));
            if (sw == null) {
                source.sendMessage(Text.literal(String.format("Backup %s is in world %s that is unsupported!", right.uid(), right.dim())).formatted(Formatting.RED));
                return false;
            }
            List<ChunkPos> chunkToUnload = ChunkPos.stream(right.begin(), right.end()).filter(it -> sw.isChunkLoaded(it.x, it.z)).toList();
            if(!chunkToUnload.isEmpty()) {
                source.sendMessage(Text.literal("These chunks are still loaded: "+chunkToUnload));
                return false;
            }
            right.entities().forEach(uid -> {
                Entity e = sw.getEntity(uid);
                if (e != null) {
                    e.setRemoved(Entity.RemovalReason.KILLED);
                }
            });
            server.saveAll(false, true, true);
            Path left = pair.getLeft();
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
                    storage
            ) {
                storage.backupRestoration(pair);
                try {
                    CompletableFuture.allOf(region.merge(), entities.merge()).join();
                    source.sendMessage(Text.literal("The restoration is done.").formatted(Formatting.GREEN));
                    return true;
                } catch (RuntimeException e) {
                    source.sendMessage(Text.literal("FAILED TO ROLLBACK CHUNKS!").formatted(Formatting.RED));
                    source.sendMessage(Text.literal("The modifies cannot be restored. Please refer to " + left + " and replace the ones in /region with them.").formatted(Formatting.RED));
                    source.sendMessage(Text.literal("Current rollback failed. However, you can still roll back to the same save when you know where the problem came.").formatted(Formatting.RED));
                    return false;
                }
            } catch (Throwable e) {
                source.sendMessage(Text.literal("We cannot roll back at this moment. Backing up of current file failed.").formatted(Formatting.RED));
                return false;
            }
        }

        @Override
        public String getInformation() {
            return "Rollback "+id;
        }
    }
}
