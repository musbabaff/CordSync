package com.blockstock.cordsync.utils;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.blockstock.cordsync.CordSync;

public class MessageUtil {

    private static final Map<String, String> cache = new HashMap<>();
    private static FileConfiguration messages;
    private static FileConfiguration fallback;
    private static String activeLanguage = "en";

    private static final String[] SUPPORTED_LANGUAGES = { "en", "tr", "de", "es", "fr" };

    public static void load(CordSync plugin) {
        String lang = plugin.getConfig().getString("language", "en").toLowerCase();
        activeLanguage = lang;

        // Save all default locale files
        for (String supported : SUPPORTED_LANGUAGES) {
            String resourceName = "locales/" + supported + ".yml";
            File localeFile = new File(plugin.getDataFolder(), resourceName);
            if (!localeFile.exists() && plugin.getResource(resourceName) != null) {
                plugin.saveResource(resourceName, false);
            }
        }

        // Also export old format for backward compatibility
        exportLegacyIfNeeded(plugin);

        // Load selected language
        File langFile = new File(plugin.getDataFolder(), "locales/" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file not found: " + lang + ".yml, falling back to en.yml");
            langFile = new File(plugin.getDataFolder(), "locales/en.yml");
            if (!langFile.exists() && plugin.getResource("locales/en.yml") != null) {
                plugin.saveResource("locales/en.yml", false);
            }
        }

        messages = YamlConfiguration.loadConfiguration(langFile);

        // Load English as fallback for missing keys
        File fallbackFile = new File(plugin.getDataFolder(), "locales/en.yml");
        if (fallbackFile.exists()) {
            fallback = YamlConfiguration.loadConfiguration(fallbackFile);
        }

        cache.clear();
        plugin.getLogger()
                .info("\uD83D\uDCAC Language loaded: " + langFile.getName() + " (" + lang.toUpperCase() + ")");
    }

    private static void exportLegacyIfNeeded(CordSync plugin) {
        // Backward compat: if old messages_TR.yml exists, don't overwrite locales
        File oldTR = new File(plugin.getDataFolder(), "messages_TR.yml");
        File oldEN = new File(plugin.getDataFolder(), "messages_EN.yml");
        if (oldTR.exists() || oldEN.exists()) {
            plugin.getLogger().info("\u2139 Legacy message files detected. Please migrate to locales/ folder.");
        }
    }

    public static String get(String key) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        String value = messages.getString(key);
        if (value == null && fallback != null) {
            value = fallback.getString(key);
        }
        if (value == null) {
            value = "&c[ERROR] Missing message key: " + key;
        }

        String colored = ChatColor.translateAlternateColorCodes('&', value);
        cache.put(key, colored);
        return colored;
    }

    public static String getRaw(String key) {
        String value = messages.getString(key);
        if (value == null && fallback != null) {
            value = fallback.getString(key);
        }
        return value != null ? value : key;
    }

    public static String format(String key, Map<String, ?> placeholders) {
        String msg = get(key);
        if (placeholders != null) {
            for (Map.Entry<String, ?> entry : placeholders.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        }
        return msg;
    }

    public static String getActiveLanguage() {
        return activeLanguage;
    }

    public static String[] getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    public static void reload(CordSync plugin) {
        cache.clear();
        load(plugin);
        plugin.getLogger().info("\uD83D\uDD04 Language reloaded.");
    }
}
