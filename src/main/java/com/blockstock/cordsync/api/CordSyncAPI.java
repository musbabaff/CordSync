package com.blockstock.cordsync.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.blockstock.cordsync.CordSync;
import com.blockstock.cordsync.managers.RewardManager;
import com.blockstock.cordsync.storage.StorageProvider;
import com.blockstock.cordsync.utils.MessageUtil;

import com.blockstock.cordsync.api.events.PlayerLinkedEvent;
import com.blockstock.cordsync.api.events.PlayerUnlinkedEvent;

public final class CordSyncAPI {

    private CordSyncAPI() {}

    private static CordSync plugin() {
        return CordSync.getInstance();
    }

    private static StorageProvider storage() {
        return plugin().getStorageProvider();
    }

    public static boolean isLinked(UUID uuid) {
        return storage().isPlayerLinked(uuid);
    }

    public static boolean isDiscordLinked(String discordId) {
        return storage().isDiscordLinked(discordId);
    }

    public static String getDiscordId(UUID uuid) {
        return storage().getDiscordId(uuid);
    }

    public static void linkAccount(UUID uuid, String playerName, String discordId) {
        storage().setLinkedAccount(uuid, playerName, discordId);
        Player player = Bukkit.getPlayer(uuid);
        Bukkit.getPluginManager().callEvent(new PlayerLinkedEvent(player, discordId));
    }

    public static void unlinkAccount(UUID uuid) {
        storage().removeLinkedAccount(uuid);
        Player player = Bukkit.getPlayer(uuid);
        Bukkit.getPluginManager().callEvent(new PlayerUnlinkedEvent(player));
    }

    public static void giveFirstReward(Player player) {
        RewardManager rm = plugin().getRewardManager();
        if (rm != null) rm.grantFirstLink(player);
    }

    public static Optional<Instant> getLastIntervalReward(UUID uuid) {
        RewardManager rm = plugin().getRewardManager();
        return (rm != null) ? rm.getLastIntervalReward(uuid) : Optional.empty();
    }

    public static void reloadAll() {
        CordSync p = plugin();
        p.reloadConfig();
        MessageUtil.reload(p);
        RewardManager rm = p.getRewardManager();
        if (rm != null) rm.reload();
    }
}


