package org.dhwpcs.inf_backup;

import org.dhwpcs.inf_backup.storage.BackupStorage;
import net.minecraft.server.command.ServerCommandSource;

import java.io.IOException;

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
            return "DeleteSave "+id;
        }
    }
}
