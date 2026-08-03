package dev.patrickjurt.borderexpander.commands;

import dev.patrickjurt.borderexpander.Main;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ItemsCommand implements SubCommand {
    private final Main plugin;

    public ItemsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "items";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        String filter = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "";
        plugin.getMissingItemsMenu().open(player, 0, filter);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

