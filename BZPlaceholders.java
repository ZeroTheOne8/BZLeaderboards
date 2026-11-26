package net.bzstudios.leaderboards;

import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/** %bzlb_<board>_<rank>_<name|value>%  and  %bzlb_<board>_rank%  (player's own rank in the top list). */
final class BZPlaceholders extends PlaceholderExpansion {

    private final BZLeaderboards plugin;

    BZPlaceholders(BZLeaderboards plugin) { this.plugin = plugin; }

    @Override public String getIdentifier() { return "bzlb"; }
    @Override public String getAuthor() { return "BZ Studios"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String[] p = params.split("_");
        Board b = plugin.board(p[0]);
        if (b == null) return null;
        List<Entry> list = plugin.snapshot().get(b.id());

        if (p.length == 2 && p[1].equalsIgnoreCase("rank")) {
            if (player == null) return "-";
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).uuid().equals(player.getUniqueId())) return String.valueOf(i + 1);
            }
            return "-";
        }
        if (p.length != 3) return null;
        int rank;
        try { rank = Integer.parseInt(p[1]); } catch (NumberFormatException e) { return null; }
        if (rank < 1 || rank > list.size()) return "-";
        Entry e = list.get(rank - 1);
        return switch (p[2].toLowerCase()) {
            case "name" -> e.name();
            case "value" -> plugin.format(b, e.value());
            default -> null;
        };
    }
}
