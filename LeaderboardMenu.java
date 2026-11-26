package net.bzstudios.leaderboards;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/** Main menu (board == null) or the top list of a single board. */
public final class LeaderboardMenu implements InventoryHolder {

    private static final int BACK_SLOT = 49;

    private final BZLeaderboards plugin;
    private final Board board;
    private final List<Board> slotBoards = new ArrayList<>();
    private Inventory inv;

    public LeaderboardMenu(BZLeaderboards plugin, Board board) {
        this.plugin = plugin;
        this.board = board;
    }

    @Override
    public Inventory getInventory() { return inv; }

    public void open(Player p) {
        if (board == null) buildMain(); else buildBoard();
        p.openInventory(inv);
    }

    private ItemStack filler() {
        Material m = Material.matchMaterial(plugin.getConfig().getString("menu.filler", "GRAY_STAINED_GLASS_PANE"));
        ItemStack it = new ItemStack(m != null ? m : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.empty());
        it.setItemMeta(meta);
        return it;
    }

    private void buildMain() {
        List<Board> all = new ArrayList<>(plugin.boards());
        int rows = Math.min(6, 2 + (int) Math.ceil(all.size() / 7.0));
        inv = Bukkit.createInventory(this, rows * 9,
                plugin.text(plugin.getConfig().getString("menu.main-title", "Leaderboards")));
        ItemStack f = filler();
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, f);

        for (int i = 0; i < all.size(); i++) {
            int slot = 10 + (i % 7) + 9 * (i / 7);
            if (slot >= inv.getSize()) break;
            Board b = all.get(i);
            ItemStack it = new ItemStack(b.icon());
            ItemMeta meta = it.getItemMeta();
            meta.displayName(plugin.text("<yellow><bold>" + b.name()));
            List<Component> lore = new ArrayList<>();
            for (String l : plugin.getConfig().getStringList("menu.board-lore")) lore.add(plugin.text(l));
            meta.lore(lore);
            it.setItemMeta(meta);
            inv.setItem(slot, it);
            slotBoards.add(b);
            // keep index aligned with slot lookup below
        }
    }

    private void buildBoard() {
        inv = Bukkit.createInventory(this, 54, plugin.text(
                plugin.getConfig().getString("menu.board-title", "Top <board>"),
                BZLeaderboards.tag("board", board.name())));
        ItemStack f = filler();
        for (int i = 45; i < 54; i++) inv.setItem(i, f);

        List<Entry> top = plugin.snapshot().get(board.id());
        if (top.isEmpty()) {
            ItemStack none = new ItemStack(Material.BARRIER);
            ItemMeta m = none.getItemMeta();
            m.displayName(plugin.msg("empty"));
            none.setItemMeta(m);
            inv.setItem(22, none);
        }
        int rank = 1;
        for (Entry e : top) {
            if (rank > 45) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(e.uuid()));
            var resolvers = new net.kyori.adventure.text.minimessage.tag.resolver.TagResolver[]{
                    BZLeaderboards.tag("rank", String.valueOf(rank)),
                    BZLeaderboards.tag("player", e.name()),
                    BZLeaderboards.tag("board", board.name()),
                    BZLeaderboards.tag("value", plugin.format(board, e.value()))};
            meta.displayName(plugin.text(plugin.getConfig().getString("menu.entry-name", "#<rank> <player>"), resolvers));
            List<Component> lore = new ArrayList<>();
            for (String l : plugin.getConfig().getStringList("menu.entry-lore")) lore.add(plugin.text(l, resolvers));
            meta.lore(lore);
            head.setItemMeta(meta);
            inv.setItem(rank - 1, head);
            rank++;
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.displayName(plugin.text(plugin.getConfig().getString("menu.back-name", "Back")));
        List<Component> lore = new ArrayList<>();
        for (String l : plugin.getConfig().getStringList("menu.back-lore")) lore.add(plugin.text(l));
        bm.lore(lore);
        back.setItemMeta(bm);
        inv.setItem(BACK_SLOT, back);
    }

    public void click(Player p, int slot) {
        if (board == null) {
            int row = slot / 9 - 1, col = slot % 9 - 1;
            if (row < 0 || col < 0 || col > 6) return;
            int idx = row * 7 + col;
            if (idx >= 0 && idx < slotBoards.size()) new LeaderboardMenu(plugin, slotBoards.get(idx)).open(p);
        } else if (slot == BACK_SLOT) {
            new LeaderboardMenu(plugin, null).open(p);
        }
    }
}
