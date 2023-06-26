package org.dhwpcs.infbackup;

import net.minecraft.server.command.ServerCommandSource;
import org.dhwpcs.infbackup.storage.BackupStorage;
import org.dhwpcs.infbackup.util.Util;

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
            return storage.doMergeDynamic(id, source.getServer(), Util.printToSource(source));
        }

        @Override
        public String getInformation() {
            return "Rollback "+id;
        }
    }
}
