package com.koopalabs.lockage;

import com.koopalabs.lockage.commands.LockageCommand;
import com.koopalabs.lockage.database.DatabaseManager;
import com.koopalabs.lockage.listeners.ChestInteractionListener;
import com.koopalabs.lockage.managers.LockManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Lockage extends JavaPlugin {
    private static Lockage instance;
    private DatabaseManager databaseManager;
    private LockManager lockManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();
        this.lockManager = new LockManager(this);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new ChestInteractionListener(this), this);
        
        // Register commands
        getCommand("lockage").setExecutor(new LockageCommand(this));
        
        getLogger().info("Lockage has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Lockage has been disabled!");
    }

    public static Lockage getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public LockManager getLockManager() {
        return lockManager;
    }
} 