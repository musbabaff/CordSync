package com.blockstock.cordsync.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.blockstock.cordsync.CordSync;

public class UpdateChecker {

    private final CordSync plugin;
    private final String repoOwner = "musbabaff";
    private final String repoName = "CordSync";
    private final String githubAPI = "https://api.github.com/repos/%s/%s/releases/latest";
    private final String repoURL = "https://github.com/%s/%s";

    public UpdateChecker(CordSync plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String apiUrl = String.format(githubAPI, repoOwner, repoName);
                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "CordSync-UpdateChecker");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    plugin.getLogger().warning("âš  GitHub API isteÄŸi baÅŸarÄ±sÄ±z! Kod: " + responseCode);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(response.toString());

                String latestVersion = (String) json.get("tag_name");
                if (latestVersion == null || latestVersion.isEmpty()) {
                    plugin.getLogger().warning("âš  GitHub yanÄ±tÄ±nda geÃ§erli bir sÃ¼rÃ¼m etiketi bulunamadÄ±.");
                    return;
                }

                String currentVersion = plugin.getDescription().getVersion();
                String repoLink = String.format(repoURL, repoOwner, repoName);

                if (isNewerVersion(latestVersion, currentVersion)) {
                    plugin.getLogger().info("Â§eâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€");
                    plugin.getLogger().info("Â§6CordSync GÃ¼ncelleme Denetimi");
                    plugin.getLogger().info("Â§cYeni sÃ¼rÃ¼m mevcut! Â§f(" + latestVersion + ")");
                    plugin.getLogger().info("Â§7YÃ¼klÃ¼ sÃ¼rÃ¼m: Â§e" + currentVersion);
                    plugin.getLogger().info("Â§aÄ°ndir: " + repoLink);
                    plugin.getLogger().info("Â§eâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€");
                } else {
                    plugin.getLogger().info("âœ… CordSync gÃ¼ncel (v" + currentVersion + ")");
                }

            } catch (Exception e) {
                plugin.getLogger().warning("âŒ GÃ¼ncelleme kontrolÃ¼ baÅŸarÄ±sÄ±z: " + e.getMessage());
            }
        });
    }

    private boolean isNewerVersion(String latest, String current) {
        try {
            String l = latest.replaceAll("[^0-9.]", "");
            String c = current.replaceAll("[^0-9.]", "");

            String[] latestParts = l.split("\\.");
            String[] currentParts = c.split("\\.");

            for (int i = 0; i < Math.max(latestParts.length, currentParts.length); i++) {
                int latestNum = (i < latestParts.length) ? Integer.parseInt(latestParts[i]) : 0;
                int currentNum = (i < currentParts.length) ? Integer.parseInt(currentParts[i]) : 0;
                if (latestNum > currentNum)
                    return true;
                if (latestNum < currentNum)
                    return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}


