package io.github.initauther97.backup;

import net.minecraft.util.math.ChunkPos;

import java.util.Date;
import java.util.List;

public record BackupInfo(ChunkPos begin, ChunkPos end, Date date, String description) {
    public List<String> serialize() {
        return List.of(
                String.valueOf(begin.toLong()),
                String.valueOf(end.toLong()),
                String.valueOf(date.getTime()),
                description
        );
    }

    public static BackupInfo deserialize(List<String> list) throws Exception {
        if(list.size() != 4) {
            throw new Exception("Wrong format");
        }
        ChunkPos begin = new ChunkPos(Long.parseLong(list.get(0)));
        ChunkPos end = new ChunkPos(Long.parseLong(list.get(1)));
        Date date = new Date(Long.parseLong(list.get(2)));
        String description = list.get(3);
        return new BackupInfo(begin,end,date,description);
    }

    @Override
    public String toString() {
        return String.format(
                "Description:'%s', Date:%s, From:%s, To:%s",
                description,
                date.toString(),
                begin.toString(),
                end.toString()
        );
    }
}
