package com.koopalabs.lockage.commands;

import com.koopalabs.lockage.Lockage;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LockageCommand implements CommandExecutor {
    private final Lockage plugin;

    public LockageCommand(Lockage plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("lockage.admin")) {
                player.sendMessage(plugin.getConfig().getString("messages.no-permission").replace("&", "§"));
                return true;
            }
            plugin.reloadConfig();
            player.sendMessage("§aConfiguration reloaded!");
            return true;
        }

        // Show help message by default
        sendHelpMessage(player);
        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Lockage BETA Help ===");
        player.sendMessage(ChatColor.RED + "Warning: This plugin is in BETA. Use with caution!");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "How to use:");
        player.sendMessage(ChatColor.GRAY + "1. Get a tripwire hook");
        player.sendMessage(ChatColor.GRAY + "2. Right-click an unlocked chest with the tripwire hook to lock it");
        player.sendMessage(ChatColor.GRAY + "3. The hook will transform into a key for that chest");
        player.sendMessage(ChatColor.GRAY + "4. Right-click the chest with the key to unlock it");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Commands:");
        if (player.hasPermission("lockage.admin")) {
            player.sendMessage(ChatColor.GRAY + "/lockage reload §f- Reload the configuration");
        }
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Tips:");
        player.sendMessage(ChatColor.GRAY + "• Each key is unique to its chest");
        player.sendMessage(ChatColor.GRAY + "• Keep your keys safe!");
        player.sendMessage(ChatColor.GRAY + "• Only the key that locked a chest can unlock it");
        player.sendMessage("");
        player.sendMessage(ChatColor.RED + "Found a bug? Report it to the server staff!");
    }
} 