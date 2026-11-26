package net.bzstudios.leaderboards;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class BZLeaderboards extends JavaPlugin {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Map<String, Board> boards = new LinkedHashMap<>();
    private final AtomicBoolean building = new AtomicBoolean(false);
    private volatile Snapshot snapshot = Snapshot.EMPTY;
    private BukkitTask task;
    private int topSize = 10;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        load();

        LeaderboardsCommand cmd = new LeaderboardsCommand(this);
        for (String name : new String[]{"leaderboards", "bzleaderboards"}) {
            Objects.requireNonNull(getCommand(name)).setExecutor(cmd);
            Objects.requireNonNull(getCommand(name)).setTabCompleter(cmd);
        }
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BZPlaceholders(this).register();
            getLogger().info("PlaceholderAPI hooked (%bzlb_<board>_<rank>_<name|value>%).");
        }
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
    }

    /** (Re)reads config and restarts the rebuild timer. */
    public void load() {
        reloadConfig();
        topSize = Math.max(1, Math.min(45, getConfig().getInt("top-size", 10)));

        boards.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("boards");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                ConfigurationSection b = sec.getConfigurationSection(id);
                if (b == null) continue;
                try {
                    Board.Source src = Board.Source.valueOf(b.getString("source", "STAT").toUpperCase(Locale.ROOT));
                    Board.Format fmt = Board.Format.valueOf(b.getString("format", "NUMBER").toUpperCase(Locale.ROOT));
                    Material icon = Material.matchMaterial(b.getString("icon", "PAPER"));
                    List<String> keys = new ArrayList<>();
                    for (String k : b.getString("key", "").split(",")) {
                        if (!k.isBlank()) keys.add(k.trim());
                    }
                    boards.put(id.toLowerCase(Locale.ROOT), new Board(
                            id.toLowerCase(Locale.ROOT), b.getString("name", id),
                            icon != null ? icon : Material.PAPER, src,
                            b.getString("category", "minecraft:custom"), keys, fmt));
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Board '" + id + "' skipped: " + ex.getMessage());
                }
            }
        }

        if (task != null) task.cancel();
        long period = Math.max(1, getConfig().getLong("rebuild-interval-minutes", 5)) * 60L * 20L;
        task = getServer().getScheduler().runTaskTimer(this, this::rebuild, 40L, period);
    }

    /** Collects main-thread-only inputs, then builds everything async. */
    public void rebuild() {
        if (!building.compareAndSet(false, true)) return;

        File statsDir = new File(getServer().getWorlds().get(0).getWorldFolder(), "stats");
        Economy eco = null;
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) eco = rsp.getProvider();
        }
        Set<UUID> hidden = new HashSet<>();
        for (String s : getConfig().getStringList("hidden-uuids")) {
            try { hidden.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
        }
        for (Player p : getServer().getOnlinePlayers()) {
            if (p.hasPermission("bzleaderboards.hide")) hidden.add(p.getUniqueId());
        }

        List<Board> list = List.copyOf(boards.values());
        Economy fEco = eco;
        int top = topSize;

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                snapshot = StatsBuilder.build(statsDir, list, fEco, hidden, top);
            } catch (Exception ex) {
                getLogger().warning("Board rebuild failed: " + ex);
            } finally {
                building.set(false);
            }
        });
    }

    // ---- accessors / helpers -------------------------------------------------

    public Snapshot snapshot() { return snapshot; }
    public Collection<Board> boards() { return boards.values(); }
    public Board board(String id) { return id == null ? null : boards.get(id.toLowerCase(Locale.ROOT)); }
    public int topSize() { return topSize; }

    /** MiniMessage -> Component with italics off by default (for item text). */
    public Component text(String mini, TagResolver... resolvers) {
        return MM.deserialize(mini, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public Component msg(String key, TagResolver... resolvers) {
        return text(getConfig().getString("messages." + key, key), resolvers);
    }

    public static TagResolver tag(String name, String value) {
        return Placeholder.unparsed(name, value);
    }

    public String format(Board b, double v) {
        return switch (b.format()) {
            case NUMBER -> String.format(Locale.US, "%,d", (long) v);
            case MONEY -> String.format(Locale.US, "$%,.2f", v);
            case DISTANCE -> {
                double km = v / 100_000.0;
                yield km >= 1 ? String.format(Locale.US, "%,.1f km", km) : String.format(Locale.US, "%,d m", (long) (v / 100));
            }
            case TIME -> {
                long s = (long) (v / 20);
                long d = s / 86400, h = s % 86400 / 3600, m = s % 3600 / 60;
                yield d > 0 ? d + "d " + h + "h " + m + "m" : h + "h " + m + "m";
            }
        };
    }
}
