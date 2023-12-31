package org.dhwpcs.infbackup.util;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class Util {
    public static <T> void forEachIndexed(T[] array, BiConsumer<Integer, T> consumer) {
        for (int i = 0; i < array.length; i++) {
            consumer.accept(i, array[i]);
        }
    }

    public static <T> void forEachIndexed(List<T> list, BiConsumer<Integer, T> consumer) {
        for (int i = 0; i < list.size(); i++) {
            consumer.accept(i, list.get(i));
        }
    }

    public static <T> void forEachIndexed(Iterable<T> iterable, BiConsumer<Integer, T> consumer) {
        Iterator<T> iter = iterable.iterator();
        int index = 0;
        while (iter.hasNext()) {
            consumer.accept(index++, iter.next());
        }
    }

    public static boolean remove(Iterable<?> iterable, int index) {
        Iterator<?> iter = iterable.iterator();
        while (true) {
            if (iter.hasNext() && index >= 0) {
                index--;
                iter.next();
            } else if (index >= 0) {
                return false;
            } else {
                iter.remove();
                return true;
            }
        }
    }

    public static <T> boolean intersects(Collection<T> first, Collection<T> second) {
        for (T f : first) {
            for (T s : second) {
                if (Objects.equals(f, s)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean within(BlockPos pos, ChunkPos pos1, ChunkPos pos2) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        boolean right1 = chunkX > pos1.x;
        boolean right2 = chunkX > pos2.x;
        boolean up1 = chunkZ > pos1.z;
        boolean up2 = chunkZ > pos2.z;
        return right1 ^ right2 && up1 ^ up2;
    }

    public static Set<ChunkPos> getBetween(ChunkPos pos1, ChunkPos pos2) {
        return ChunkPos.stream(pos1, pos2).collect(Collectors.toSet());
    }

    @SafeVarargs
    public static<T> Iterable<T> asIterable(T... array) {
        Objects.requireNonNull(array);
        return () -> new Iterator<>() {

            private final T[] arr = array.clone();
            private final int len = arr.length;
            private int pointer = 0;
            @Override
            public boolean hasNext() {
                return pointer < len;
            }

            @Override
            public T next() {
                if(!hasNext()) {
                    throw new NoSuchElementException();
                }
                return arr[pointer++];
            }
        };
    }

    public static FileVisitor<Path> deleteDirectory() {
        return new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                exc.printStackTrace();
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        };
    }

    public static BiConsumer<Level, String> printToSource(ServerCommandSource source) {
        return (level, s) -> {
            if(level.isInRange(Level.DEBUG, Level.INFO)) {
                source.sendMessage(Text.of(s));
            } else if(level == Level.WARN) {
                source.sendMessage(Text.literal(s).formatted(Formatting.YELLOW));
            } else {
                source.sendMessage(Text.literal(s).formatted(Formatting.RED));
            }
        };
    }
}
