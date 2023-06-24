package org.dhwpcs.infbackup.storage;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import java.util.*;

public record BackupInfo(UUID uid, Identifier dim, ChunkPos begin, ChunkPos end, Date date, String description,
                         List<UUID> entities) {
    public List<String> serialize() {
        LinkedList<String> list = new LinkedList<>();
        list.add(uid.toString());
        list.add(dim.toString());
        list.add(String.valueOf(begin.toLong()));
        list.add(String.valueOf(end.toLong()));
        list.add(String.valueOf(date.getTime()));
        list.add(description);
        entities.forEach(it -> list.add(it.toString()));
        return list;
    }

    public static BackupInfo deserialize(List<String> list) throws Exception {
        if (list.size() < 6) {
            throw new Exception("Wrong format");
        }
        Iterator<String> iter = list.iterator();
        //6 reads
        UUID uid = UUID.fromString(iter.next());
        Identifier dim = Identifier.tryParse(iter.next());
        ChunkPos begin = new ChunkPos(Long.parseLong(iter.next()));
        ChunkPos end = new ChunkPos(Long.parseLong(iter.next()));
        Date date = new Date(Long.parseLong(iter.next()));
        String description = iter.next();

        List<UUID> uids = new ArrayList<>(list.size() - 6);
        iter.forEachRemaining(it -> uids.add(UUID.fromString(it)));
        return new BackupInfo(uid, dim, begin, end, date, description, uids);
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
