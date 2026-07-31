package dev.patrickjurt.borderexpander.listeners;

import dev.patrickjurt.borderexpander.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLevelChangeEvent;

public final class PlayerLevelChangeListener implements Listener {
    private final Main plugin;

    public PlayerLevelChangeListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        plugin.handlePlayerLevelChange();
    }
}

