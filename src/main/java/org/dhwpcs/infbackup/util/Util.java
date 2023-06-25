package org.dhwpcs.infbackup.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.apache.logging.log4j.Level;
import org.dhwpcs.infbackup.storage.Backup;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;

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
        Backup.LOGGER.log(Level.INFO, "{},{}", chunkX, chunkZ);
        boolean right1 = chunkX > pos1.x;
        boolean right2 = chunkX > pos2.x;
        boolean up1 = chunkZ > pos1.z;
        boolean up2 = chunkZ > pos2.z;
        boolean result = right1 ^ right2 && up1 ^ up2;
        Backup.LOGGER.log(Level.INFO, result);
        return result;
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
}