package org.dhwpcs.inf_backup.storage;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.storage.StorageIoWorker;
import org.dhwpcs.inf_backup.mixin.StorageIoWorkerAccessor;
import org.dhwpcs.inf_backup.util.Signal;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class RegionMerger implements Closeable {

    private static final Signal NO_DATA = new Signal("no_data");

    private final StorageIoWorker sFrom;
    private final StorageIoWorker sTo;
    private final ChunkPos begin;
    private final ChunkPos end;

    private CompletableFuture<Void> currentFuture;

    public RegionMerger(Path from, Path to, ChunkPos begin, ChunkPos end) {
        sFrom = StorageIoWorkerAccessor.create(from, false, "Merge-Origin");
        sTo = StorageIoWorkerAccessor.create(to, false,"Merge-Target");
        this.begin = begin;
        this.end = end;
    }

    public CompletableFuture<Void> merge() {
        if(currentFuture != null) {
            return currentFuture;
        }
        return currentFuture = CompletableFuture.allOf(ChunkPos.stream(begin, end)
                .map(pos -> ((StorageIoWorkerAccessor)sFrom).fetchChunkData(pos)
                        .handle((optional, t) -> {
                            if(optional.isEmpty()) {
                                if(t == null) {
                                    Backup.LOGGER.info("Chunk {} does not have any data.", pos);
                                    throw NO_DATA;
                                } else {
                                    throw new RuntimeException(t);
                                }
                            } else {
                                return optional.get();
                            }
                        })
                        .thenCompose(compound -> sTo.setResult(pos, compound))
                        .handle((unused, t) -> {
                            if (t == null || t.getCause() == NO_DATA) {
                                Backup.LOGGER.info("Merged chunk {}", pos.toString());
                            } else {
                                Backup.LOGGER.fatal("Failed to merge chunk {}", pos.toString(), t);
                            }
                            return unused;
                        })
                ).toArray(CompletableFuture[]::new));
    }

    @Override
    public void close() throws IOException {
        if(currentFuture != null) {
            currentFuture.join();
        } else {
            (currentFuture = sTo.completeAll(false)).join();
        }
        sFrom.close();
        sTo.close();
    }

}
