package org.dhwpcs.inf_backup.storage;

import org.dhwpcs.inf_backup.mixin.StorageIoWorkerAccessor;
import org.dhwpcs.inf_backup.util.RegionPos;
import org.dhwpcs.inf_backup.util.Signal;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.storage.StorageIoWorker;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RegionMerger implements Closeable {

    private static final Signal NO_DATA = new Signal("no_data");

    private final StorageIoWorker sFrom;
    private final StorageIoWorker sTo;
    private final Map<RegionPos, List<ChunkPos>> chunks;

    private CompletableFuture<Void> currentFuture;

    public RegionMerger(Path from, Path to, ChunkPos begin, ChunkPos end) {
        sFrom = StorageIoWorkerAccessor.create(from, false, "Merge-Origin");
        sTo = StorageIoWorkerAccessor.create(to, false,"Merge-Target");
        chunks = ChunkPos.stream(begin, end).parallel().collect(Collectors.groupingBy(RegionPos::get));
    }

    public CompletableFuture<Void> merge() {
        if(currentFuture != null) {
            return currentFuture;
        }
        return currentFuture = CompletableFuture.allOf(chunks.entrySet().stream().flatMap(entry -> entry.getValue()
                .stream()
                .map(pos -> ((StorageIoWorkerAccessor)sFrom)
                        .fetchChunkData(pos)
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
                )
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
