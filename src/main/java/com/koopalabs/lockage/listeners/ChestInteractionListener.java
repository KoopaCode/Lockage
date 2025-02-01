package com.koopalabs.lockage.listeners;

import com.koopalabs.lockage.Lockage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ChestInteractionListener implements Listener {
    private final Lockage plugin;

    public ChestInteractionListener(Lockage plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChestInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        
        if (block == null || block.getType() != Material.CHEST) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        boolean hasItem = item != null && !item.getType().equals(Material.AIR);

        // Always cancel the event first to prevent chest opening
        event.setCancelled(true);

        // Check if chest is locked
        if (plugin.getDatabaseManager().isChestLocked(block.getLocation())) {
            // If they have a valid key, let them unlock
            if (hasItem && item.getType() == Material.TRIPWIRE_HOOK) {
                if (plugin.getLockManager().canUnlock(block.getLocation(), item)) {
                    plugin.getLockManager().unlockChest(block.getLocation());
                    player.sendMessage(plugin.getConfig().getString("messages.chest-unlocked").replace("&", "§"));
                    player.playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
                    player.playSound(block.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.5f, 1.0f);
                    event.setCancelled(false); // Allow chest to open
                } else {
                    player.sendMessage(plugin.getConfig().getString("messages.invalid-key").replace("&", "§"));
                    player.playSound(block.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 0.5f);
                }
            } else {
                player.sendMessage(plugin.getConfig().getString("messages.chest-protected").replace("&", "§"));
                player.playSound(block.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 0.5f);
            }
            return;
        }

        // Chest is unlocked - check if they're trying to lock it with a tripwire hook
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && 
            hasItem && item.getType() == Material.TRIPWIRE_HOOK) {
            
            // Remove one tripwire hook
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            
            // Create and give the key
            ItemStack key = plugin.getLockManager().createKeyForChest(player, block.getLocation());
            player.getInventory().addItem(key);
            player.sendMessage(plugin.getConfig().getString("messages.chest-locked").replace("&", "§"));
            player.playSound(block.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
            player.playSound(block.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.5f, 1.0f);
        } else {
            // Allow normal chest interaction if not trying to lock
            event.setCancelled(false);
        }
    }

    @EventHandler
    public void onChestPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.CHEST) {
            // Chests are unlocked by default
            return;
        }
    }
} 