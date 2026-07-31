package dev.patrickjurt.borderexpander.commands;

import dev.patrickjurt.borderexpander.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BorderExpanderCommand implements CommandExecutor, TabCompleter {
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public BorderExpanderCommand(Main plugin) {
        registerSubCommand(new ItemsCommand(plugin));
        registerSubCommand(new StatsCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.name(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /" + label + " <" + String.join("|", subCommands.keySet()) + ">");
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            sender.sendMessage("Usage: /" + label + " <" + String.join("|", subCommands.keySet()) + ">");
            return true;
        }

        return subCommand.execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        String token = args[0].toLowerCase(Locale.ROOT);
        return subCommands.keySet().stream()
            .filter(option -> option.startsWith(token))
            .toList();
    }
}

