package com.blockstock.cordsync.rewards;

import java.util.UUID;

import com.blockstock.cordsync.CordSync;
import com.blockstock.cordsync.rewards.storage.MySQLRewardLogStorage;
import com.blockstock.cordsync.rewards.storage.RewardLogStorage;
import com.blockstock.cordsync.rewards.storage.SQLiteRewardLogStorage;
import com.blockstock.cordsync.rewards.storage.YamlRewardLogStorage;


public class RewardLogManager {

    private final CordSync plugin;
    private RewardLogStorage logStorage;

    public RewardLogManager(CordSync plugin) {
        this.plugin = plugin;
        initializeStorage();
    }

  
    private void initializeStorage() {
        String type = plugin.getConfig().getString("rewards.log-storage", "YAML").toUpperCase();

        switch (type) {
            case "MYSQL" -> {
                plugin.getLogger().info("ğŸ’¾ Ã–dÃ¼l log depolama: MySQL");
                this.logStorage = new MySQLRewardLogStorage(plugin);
            }
            case "SQLITE" -> {
                plugin.getLogger().info("ğŸ’¾ Ã–dÃ¼l log depolama: SQLite");
                this.logStorage = new SQLiteRewardLogStorage(plugin);
            }
            default -> {
                plugin.getLogger().info("ğŸ’¾ Ã–dÃ¼l log depolama: YAML (varsayÄ±lan)");
                this.logStorage = new YamlRewardLogStorage(plugin);
            }
        }
    }


    public void logReward(UUID player, String rewardType, String details) {
        if (logStorage == null) {
            plugin.getLogger().warning("Ã–dÃ¼l log sistemi baÅŸlatÄ±lamadÄ±! Log atlanÄ±yor.");
            return;
        }
        logStorage.log(player, rewardType, details);
    }


    public void close() {
        if (logStorage != null) {
            logStorage.close();
        }
    }
}


