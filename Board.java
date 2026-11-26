package net.bzstudios.leaderboards;

import java.util.List;
import org.bukkit.Material;

/** One configured leaderboard. */
public record Board(String id, String name, Material icon, Source source,
                    String category, List<String> keys, Format format) {

    public enum Source { STAT, SUM, VAULT }

    public enum Format { NUMBER, TIME, MONEY, DISTANCE }
}
