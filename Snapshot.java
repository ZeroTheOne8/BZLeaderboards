package net.bzstudios.leaderboards;

import java.util.List;
import java.util.Map;

/** Immutable finished result of one rebuild; swapped in atomically. */
public record Snapshot(Map<String, List<Entry>> boards, long builtAt) {
    public static final Snapshot EMPTY = new Snapshot(Map.of(), 0L);

    public List<Entry> get(String id) {
        return boards.getOrDefault(id, List.of());
    }
}
