package com.koopalabs.lockage.managers;

import com.koopalabs.lockage.Lockage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.UUID;

public class LockManager {
    private final Lockage plugin;

    public LockManager(Lockage plugin) {
        this.plugin = plugin;
    }

    public ItemStack createKeyForChest(Player player, Location chestLocation) {
        try {
            ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK);
            ItemMeta meta = key.getItemMeta();
            String keyId = UUID.randomUUID().toString().substring(0, 8);
            
            meta.setDisplayName("§6Chest Key");
            meta.setLore(Arrays.asList(
                "§7Owner: " + player.getName(),
                "§7Key ID: " + keyId,
                "§7Location: " + formatLocation(chestLocation)
            ));
            key.setItemMeta(meta);
            
            // Try to lock the chest
            try {
                plugin.getDatabaseManager().addLockedChest(chestLocation, player.getUniqueId(), keyId);
            } catch (Exception e) {
                player.sendMessage("§cFailed to lock chest! Please contact an administrator.");
                plugin.getLogger().severe("Failed to create key: " + e.getMessage());
                return null;
            }
            
            return key;
        } catch (Exception e) {
            player.sendMessage("§cAn error occurred while creating the key!");
            plugin.getLogger().severe("Failed to create key item: " + e.getMessage());
            return null;
        }
    }

    private String formatLocation(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    public boolean isValidKey(ItemStack key) {
        if (key == null || key.getType() != Material.TRIPWIRE_HOOK) {
            return false;
        }
        
        if (!key.hasItemMeta() || !key.getItemMeta().hasLore()) {
            return false;
        }

        return getKeyId(key) != null;
    }

    private String getKeyId(ItemStack key) {
        if (key != null && key.hasItemMeta() && key.getItemMeta().hasLore()) {
            for (String lore : key.getItemMeta().getLore()) {
                if (lore.startsWith("§7Key ID: ")) {
                    return lore.substring(9);
                }
            }
        }
        return null;
    }

    public boolean canUnlock(Location location, ItemStack key) {
        try {
            if (!isValidKey(key)) {
                return false;
            }
            
            String keyId = getKeyId(key);
            return plugin.getDatabaseManager().isKeyValidForChest(location, keyId);
        } catch (Exception e) {
            plugin.getLogger().severe("Error checking key validity: " + e.getMessage());
            return false;
        }
    }

    public void unlockChest(Location location) {
        plugin.getDatabaseManager().removeLock(location);
    }
} 