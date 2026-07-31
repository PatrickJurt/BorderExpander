package dev.patrickjurt.borderexpander.inventories;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public record MissingItemsMenuHolder(UUID playerId, int page) implements InventoryHolder {
    private static final int MENU_SIZE = 54;

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, MENU_SIZE);
    }
}

