package dev.patrickjurt.borderexpander.commands;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {
    String name();

    boolean execute(CommandSender sender, String[] args);

    List<String> tabComplete(CommandSender sender, String[] args);
}

