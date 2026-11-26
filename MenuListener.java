package net.bzstudios.leaderboards;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

final class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof LeaderboardMenu menu)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == e.getView().getTopInventory() && e.getWhoClicked() instanceof Player p) {
            menu.click(p, e.getSlot());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof LeaderboardMenu) e.setCancelled(true);
    }
}
