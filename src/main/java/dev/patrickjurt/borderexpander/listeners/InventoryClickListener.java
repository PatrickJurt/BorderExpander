package dev.patrickjurt.borderexpander.listeners;

import dev.patrickjurt.borderexpander.Main;
import dev.patrickjurt.borderexpander.inventories.MissingItemsMenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class InventoryClickListener implements Listener {
    private static final int MENU_SIZE = 54;

    private final Main plugin;

    public InventoryClickListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MissingItemsMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!holder.playerId().equals(player.getUniqueId()) || event.getRawSlot() >= MENU_SIZE) {
            return;
        }

        if (event.getRawSlot() == 45) {
            plugin.getMissingItemsMenu().open(player, holder.page() - 1, holder.filter());
            return;
        }

        if (event.getRawSlot() == 53) {
            plugin.getMissingItemsMenu().open(player, holder.page() + 1, holder.filter());
        }
    }
}

