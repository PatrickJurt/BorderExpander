package dev.patrickjurt.borderexpander.listeners;

import dev.patrickjurt.borderexpander.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public final class WorldLoadListener implements Listener {
    private final Main plugin;

    public WorldLoadListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.handleWorldLoad(event.getWorld());
    }
}

