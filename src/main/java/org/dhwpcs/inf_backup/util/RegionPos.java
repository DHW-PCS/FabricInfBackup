package org.dhwpcs.inf_backup.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("unused")
public record RegionPos(int x, int z) {
    public static RegionPos get(BlockPos pos) {
        return new RegionPos(
                ((int) Math.floor(pos.getX() / 16.0)) >> 5,
                ((int) Math.floor(pos.getZ() / 16.0)) >> 5
        );
    }

    public static RegionPos get(ChunkPos pos) {
        return new RegionPos(pos.getRegionX(), pos.getRegionZ());
    }

    public static RegionPos get(double px, double pz) {
        return new RegionPos(
                ((int) Math.floor(px / 16)) >> 5,
                ((int) Math.floor(pz / 16)) >> 5
        );
    }

    public String getFileName() {
        return "r." + x + "." + z + ".mca";
    }

    public static Stream<RegionPos> betweenClosed(RegionPos pos1, RegionPos pos2) {
        int i = Math.abs(pos1.x - pos2.x) + 1;
        int j = Math.abs(pos1.z - pos2.z) + 1;
        final int k = pos1.x < pos2.x ? 1 : -1;
        final int l = pos1.z < pos2.z ? 1 : -1;
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<>((long) i * j, Spliterator.SIZED) {
            @Nullable
            private RegionPos position;

            public boolean tryAdvance(Consumer<? super RegionPos> consumer) {
                if (this.position == null) {
                    this.position = pos1;
                } else {
                    int i = this.position.x;
                    int j = this.position.z;
                    if (i == pos2.x) {
                        if (j == pos2.z) {
                            return false;
                        }

                        this.position = new RegionPos(pos1.x, j + l);
                    } else {
                        this.position = new RegionPos(i + k, j);
                    }
                }

                consumer.accept(this.position);
                return true;
            }
        }, false);
    }

    public static Stream<RegionPos> betweenClosed(ChunkPos pos1, ChunkPos pos2) {
        return betweenClosed(RegionPos.get(pos1), RegionPos.get(pos2));
    }

    public boolean within(RegionPos pos1, RegionPos pos2) {
        boolean right1 = x > pos1.x;
        boolean right2 = x > pos2.x;
        boolean up1 = z > pos1.z;
        boolean up2 = z > pos2.z;
        return right1 ^ right2 && up1 ^ up2;
    }

    public boolean contains(ChunkPos pos) {
        return pos.getRegionX() == x && pos.getRegionZ() == z;
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >> 9 == x && pos.getZ() >> 9 == z;
    }

    public boolean contains(double px, double pz) {
        return (int) px >> 9 == x && (int) pz >> 9 == z;
    }
}
