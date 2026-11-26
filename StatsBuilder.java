package net.bzstudios.leaderboards;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.util.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/** Builds every board on an async thread. Only reads files, never the world. */
final class StatsBuilder {

    private StatsBuilder() { }

    static Snapshot build(File statsDir, Collection<Board> boards, Economy eco,
                          Set<UUID> hidden, int top) {
        Map<String, List<Entry>> raw = new LinkedHashMap<>();
        for (Board b : boards) raw.put(b.id(), new ArrayList<>());

        File[] files = statsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                UUID id;
                try {
                    String n = f.getName();
                    id = UUID.fromString(n.substring(0, n.length() - 5));
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (hidden.contains(id)) continue;

                JsonObject stats;
                try (Reader r = Files.newBufferedReader(f.toPath())) {
                    JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                    stats = root.has("stats") ? root.getAsJsonObject("stats") : new JsonObject();
                } catch (Exception e) {
                    continue; // unreadable / half-written file
                }

                OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                String name = op.getName() != null ? op.getName() : id.toString().substring(0, 8);

                for (Board b : boards) {
                    double v = switch (b.source()) {
                        case VAULT -> balance(eco, op);
                        case SUM -> sum(stats, b.category());
                        case STAT -> stat(stats, b.category(), b.keys());
                    };
                    if (v > 0) raw.get(b.id()).add(new Entry(id, name, v));
                }
            }
        }

        Map<String, List<Entry>> out = new LinkedHashMap<>();
        for (var e : raw.entrySet()) {
            List<Entry> list = e.getValue();
            list.sort(Comparator.comparingDouble(Entry::value).reversed());
            out.put(e.getKey(), List.copyOf(list.subList(0, Math.min(top, list.size()))));
        }
        return new Snapshot(Map.copyOf(out), System.currentTimeMillis());
    }

    private static double balance(Economy eco, OfflinePlayer p) {
        if (eco == null) return 0;
        try {
            return eco.getBalance(p);
        } catch (Exception e) {
            return 0;
        }
    }

    private static double stat(JsonObject stats, String category, List<String> keys) {
        if (!stats.has(category)) return 0;
        JsonObject cat = stats.getAsJsonObject(category);
        for (String k : keys) {
            JsonElement el = cat.get(k);
            if (el != null && el.isJsonPrimitive()) return el.getAsDouble();
        }
        return 0;
    }

    private static double sum(JsonObject stats, String category) {
        if (!stats.has(category)) return 0;
        double total = 0;
        for (var e : stats.getAsJsonObject(category).entrySet()) {
            if (e.getValue().isJsonPrimitive()) total += e.getValue().getAsDouble();
        }
        return total;
    }
}
