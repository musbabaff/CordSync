package com.blockstock.mlinker.rewards.storage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.blockstock.mlinker.MLinker;


public class YamlRewardLogStorage implements RewardLogStorage {

    private final MLinker plugin;
    private final File file;
    private final FileConfiguration data;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public YamlRewardLogStorage(MLinker plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "reward-logs.yml");

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                plugin.getLogger().info("✅ reward-logs.yml oluşturuldu.");
            } catch (IOException e) {
                plugin.getLogger().severe("❌ reward-logs.yml oluşturulamadı: " + e.getMessage());
            }
        }

        this.data = YamlConfiguration.loadConfiguration(file);
    }

 
    @Override
    public void log(UUID player, String rewardType, String details) {
        String uuidStr = player.toString();
        String timestamp = dateFormat.format(new Date());
        String path = "logs." + uuidStr + "." + timestamp;

        data.set(path + ".reward-type", rewardType);
        data.set(path + ".details", details);

        save();
        plugin.getLogger().info("🧾 [" + rewardType + "] ödülü loglandı: " + uuidStr);
    }

   
    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("❌ reward-logs.yml kaydedilemedi: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        save();
    }
}
