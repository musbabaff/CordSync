package com.blockstock.cordsync.listeners;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.blockstock.cordsync.CordSync;
import com.blockstock.cordsync.utils.MessageUtil;

/**
 * Chat Bridge: Relays Minecraft chat messages to a Discord channel.
 * Discord → MC is handled in DiscordBot.java
 */
public class ChatBridgeListener implements Listener {

    private final CordSync plugin;

    public ChatBridgeListener(CordSync plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-bridge.enabled", false))
            return;

        Player player = event.getPlayer();
        String message = event.getMessage();

        // Check ignored prefixes
        List<String> ignoredPrefixes = plugin.getConfig().getStringList("chat-bridge.ignored-prefixes");
        for (String prefix : ignoredPrefixes) {
            if (message.startsWith(prefix))
                return;
        }

        // Check MC → Discord permission
        String mcToDiscord = plugin.getConfig().getString("chat-bridge.mc-to-discord", "ALL");
        if ("LINKED_ONLY".equalsIgnoreCase(mcToDiscord)) {
            if (!plugin.getStorageProvider().isPlayerLinked(player.getUniqueId())) {
                return;
            }
        }

        // Permission check
        if (!player.hasPermission("cordsync.chat"))
            return;

        // Send to Discord
        if (plugin.getDiscordBot() != null) {
            String format = plugin.getConfig().getString("chat-bridge.discord-format",
                    "**{player}**: {message}");
            String formatted = format
                    .replace("{player}", player.getName())
                    .replace("{message}", sanitizeDiscordMessage(message));

            plugin.getDiscordBot().sendChatBridgeMessage(formatted);
        }
    }

    /**
     * Sanitizes message content to prevent Discord mentions and exploits.
     */
    private String sanitizeDiscordMessage(String message) {
        return message
                .replace("@everyone", "@\u200Beveryone")
                .replace("@here", "@\u200Bhere")
                .replaceAll("<@!?\\d+>", "[mention]")
                .replaceAll("<@&\\d+>", "[role]");
    }
}
