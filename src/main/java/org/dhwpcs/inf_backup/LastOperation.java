package org.dhwpcs.inf_backup;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Pair;
import org.dhwpcs.inf_backup.storage.BackupInfo;
import org.dhwpcs.inf_backup.storage.BackupStorage;

import java.io.IOException;
import java.nio.file.Path;

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

    class Rollback implements LastOperation {
        private final int id;
        private final BackupStorage storage;

        public Rollback(int id, BackupStorage storage) {
            this.id = id;
            this.storage = storage;
        }

        @Override
        public boolean perform(ServerCommandSource source) {
            MinecraftServer server = source.getServer();
            Pair<Path, BackupInfo> pair = storage.find(id);
            
        }

        @Override
        public String getInformation() {
            return "Rollback "+id;
        }
    }
}
