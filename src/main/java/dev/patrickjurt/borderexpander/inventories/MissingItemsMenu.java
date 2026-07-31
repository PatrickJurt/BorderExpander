package dev.patrickjurt.borderexpander.inventories;

import dev.patrickjurt.borderexpander.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class MissingItemsMenu {
    public static final int MENU_SIZE = 54;
    public static final int PAGE_SIZE = 45;

    private final Main plugin;

    public MissingItemsMenu(Main plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int requestedPage) {
        List<Material> missingItems = getMissingItemsForPlayer(player);
        int pageCount = Math.max(1, (int) Math.ceil(missingItems.size() / (double) PAGE_SIZE));
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));

        Inventory inventory = Bukkit.createInventory(new MissingItemsMenuHolder(player.getUniqueId(), page), MENU_SIZE,
            "Missing Items " + (page + 1) + "/" + pageCount);

        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, missingItems.size());
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int itemIndex = startIndex + slot;
            if (itemIndex >= endIndex) {
                break;
            }

            Material material = missingItems.get(itemIndex);
            ItemStack itemStack = new ItemStack(material);
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(plugin.prettyMaterialName(material));
            itemStack.setItemMeta(itemMeta);
            inventory.setItem(slot, itemStack);
        }

        if (page > 0) {
            inventory.setItem(45, createPageButton("Previous page"));
        }
        if (page < pageCount - 1) {
            inventory.setItem(53, createPageButton("Next page"));
        }

        player.openInventory(inventory);
    }

    private ItemStack createPageButton(String title) {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(title);
        button.setItemMeta(meta);
        return button;
    }

    private List<Material> getMissingItemsForPlayer(Player player) {
        Set<Material> found = plugin.getFoundItems(player.getUniqueId());
        return plugin.getAllTrackableItems().stream()
            .filter(material -> !found.contains(material))
            .sorted(Comparator.comparing(Material::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }
}

