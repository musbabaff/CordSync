package com.blockstock.mlinker.rewards;

import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;

import com.blockstock.mlinker.MLinker;
import com.blockstock.mlinker.rewards.storage.MySQLRewardLogStorage;
import com.blockstock.mlinker.rewards.storage.RewardLogStorage;
import com.blockstock.mlinker.rewards.storage.SQLiteRewardLogStorage;
import com.blockstock.mlinker.rewards.storage.YamlRewardLogStorage;


public class RewardLogger {

    private final MLinker plugin;
    private final RewardLogStorage storage;

    public RewardLogger(MLinker plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("reward-logs.type", "YAML").toUpperCase();

        switch (type) {
            case "MYSQL" -> {
                plugin.getLogger().info("💾 Ödül loglama yöntemi: MySQL");
                storage = new MySQLRewardLogStorage(plugin);
            }
            case "SQLITE" -> {
                plugin.getLogger().info("💾 Ödül loglama yöntemi: SQLite");
                storage = new SQLiteRewardLogStorage(plugin);
            }
            default -> {
                plugin.getLogger().info("💾 Ödül loglama yöntemi: YAML (varsayılan)");
                storage = new YamlRewardLogStorage(plugin);
            }
        }
    }


    public void logReward(UUID player, String rewardType, String details) {
        if (storage == null) {
            plugin.getLogger().warning("RewardLogger aktif değil, loglama atlandı.");
            return;
        }

        try {
            storage.log(player, rewardType, details);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Ödül loglama hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        if (storage != null) {
            storage.close();
        }
    }
}
