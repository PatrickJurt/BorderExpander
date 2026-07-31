package dev.patrickjurt.borderexpander.commands;

import dev.patrickjurt.borderexpander.Main;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public final class StatsCommand implements SubCommand {
    private final Main plugin;

    public StatsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "stats";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.sendStats(sender);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

