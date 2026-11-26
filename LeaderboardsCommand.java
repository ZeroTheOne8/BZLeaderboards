package net.bzstudios.leaderboards;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

final class LeaderboardsCommand implements CommandExecutor, TabCompleter {

    private final BZLeaderboards plugin;

    LeaderboardsCommand(BZLeaderboards plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (cmd.getName().equalsIgnoreCase("bzleaderboards")) return admin(s, a);

        if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
        if (!p.hasPermission("bzleaderboards.use")) { p.sendMessage(plugin.msg("no-permission")); return true; }

        if (a.length == 0) { new LeaderboardMenu(plugin, null).open(p); return true; }
        Board b = plugin.board(a[0]);
        if (b == null) { p.sendMessage(plugin.msg("unknown-board", BZLeaderboards.tag("board", a[0]))); return true; }
        new LeaderboardMenu(plugin, b).open(p);
        return true;
    }

    private boolean admin(CommandSender s, String[] a) {
        if (!s.hasPermission("bzleaderboards.admin")) { s.sendMessage(plugin.msg("no-permission")); return true; }
        String sub = a.length == 0 ? "status" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> { plugin.load(); plugin.rebuild(); s.sendMessage(plugin.msg("reloaded")); }
            case "rebuild" -> { plugin.rebuild(); s.sendMessage(plugin.msg("rebuilding")); }
            default -> {
                long t = plugin.snapshot().builtAt();
                String age = t == 0 ? "never" : ((System.currentTimeMillis() - t) / 1000) + "s ago";
                s.sendMessage(plugin.msg("status",
                        BZLeaderboards.tag("boards", String.valueOf(plugin.boards().size())),
                        BZLeaderboards.tag("age", age)));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length != 1) return out;
        if (cmd.getName().equalsIgnoreCase("bzleaderboards")) {
            out.addAll(List.of("reload", "rebuild", "status"));
        } else {
            plugin.boards().forEach(b -> out.add(b.id()));
        }
        out.removeIf(x -> !x.startsWith(a[0].toLowerCase(Locale.ROOT)));
        return out;
    }
}
