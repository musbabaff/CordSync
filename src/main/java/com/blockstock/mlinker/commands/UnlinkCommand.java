package com.blockstock.mlinker.commands;

import java.awt.Color;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.blockstock.mlinker.MLinker;
import com.blockstock.mlinker.storage.StorageProvider;
import com.blockstock.mlinker.utils.MessageUtil;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

public class UnlinkCommand implements CommandExecutor {

    private final MLinker plugin;
    private final StorageProvider storage;

    public UnlinkCommand() {
        this.plugin = MLinker.getInstance();
        this.storage = plugin.getStorageProvider();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.get("link.not-player"));
            return true;
        }

        Player player = (Player) sender;

        // Yetki kontrolü
        if (!player.hasPermission("mlinker.use")) {
            player.sendMessage(MessageUtil.get("system.no-permission"));
            return true;
        }

        if (!storage.isPlayerLinked(player.getUniqueId())) {
            player.sendMessage(MessageUtil.get("unlink.not-linked"));
            return true;
        }

        String discordId = storage.getDiscordId(player.getUniqueId());
        if (discordId == null || discordId.isEmpty()) {
            player.sendMessage(MessageUtil.get("unlink.error"));
            return true;
        }

        // Veritabanı ve Discord API işlemlerini tamamen Asenkron yapıyoruz.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            // 1. Veritabanından silme işlemi
            storage.removeLinkedAccount(player.getUniqueId());

            // 2. Discord rolünü alma işlemi
            removeVerifiedRole(discordId);

            // 3. LuckPerms eşleme rollerini kaldır
            removeLuckPermsRoles(discordId, player.getUniqueId());

            // 4. Log kanalına bilgilendirme gönder
            sendUnlinkLog(player.getName());

            // 5. Oyuncuya ve konsola mesaj gönderme
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(MessageUtil.get("unlink.success"));
                }
                plugin.getLogger()
                        .info(MessageUtil.format("unlink.console-success", Map.of("player", player.getName())));
            });

        });

        return true;
    }

    private void removeVerifiedRole(String discordId) {
        try {
            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null) {
                plugin.getLogger().warning(MessageUtil.get("discord.bot-disabled"));
                return;
            }

            String guildId = plugin.getConfig().getString("discord.guild-id");
            String roleId = plugin.getConfig().getString("discord.role-id-verified");

            if (guildId == null || roleId == null || guildId.isEmpty() || roleId.isEmpty()) {
                plugin.getLogger().warning(MessageUtil.get("discord.role-missing"));
                return;
            }

            Guild guild = plugin.getDiscordBot().getJda().getGuildById(guildId);
            if (guild == null) {
                plugin.getLogger().warning(MessageUtil.get("discord.guild-missing"));
                return;
            }

            Role role = guild.getRoleById(roleId);
            if (role == null) {
                plugin.getLogger().warning(MessageUtil.get("discord.role-missing"));
                return;
            }

            if (discordId == null || discordId.isEmpty())
                return;

            Member member = guild.retrieveMemberById(discordId).complete();
            if (member == null) {
                plugin.getLogger().warning(MessageUtil.format("discord.user-not-found", Map.of("id", discordId)));
                return;
            }

            guild.removeRoleFromMember(member, role).queue(
                    success -> plugin.getLogger().info(MessageUtil.get("discord.unlinked")),
                    error -> plugin.getLogger().warning(MessageUtil.format("discord.role-fail",
                            Map.of("error", error.getMessage()))));

        } catch (Exception e) {
            plugin.getLogger().severe(MessageUtil.format("discord.role-remove-error", Map.of("error", e.getMessage())));
        }
    }

    /**
     * LuckPerms grup→Discord rol eşlemelerindeki rolleri kaldırır.
     */
    private void removeLuckPermsRoles(String discordId, UUID playerUUID) {
        try {
            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
                return;

            if (!plugin.getConfig().getBoolean("discord.luckperms-roles.enabled", false))
                return;

            ConfigurationSection mappings = plugin.getConfig()
                    .getConfigurationSection("discord.luckperms-roles.mappings");
            if (mappings == null || mappings.getKeys(false).isEmpty())
                return;

            // Guild bilgisi
            String guildId = plugin.getConfig().getString("discord.guild-id");
            if (guildId == null || guildId.isEmpty())
                return;

            Guild guild = plugin.getDiscordBot().getJda().getGuildById(guildId);
            if (guild == null)
                return;
            if (discordId == null || discordId.isEmpty())
                return;

            Member member = guild.retrieveMemberById(discordId).complete();
            if (member == null)
                return;

            // LuckPerms API'sine erişim (oyuncunun gruplarını öğrenmek için)
            LuckPerms luckPerms = null;
            Set<String> groups = new java.util.HashSet<>();
            try {
                var provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
                if (provider != null) {
                    luckPerms = provider.getProvider();
                    User lpUser = luckPerms.getUserManager().getUser(playerUUID);
                    if (lpUser == null) {
                        lpUser = luckPerms.getUserManager().loadUser(playerUUID).join();
                    }
                    if (lpUser != null) {
                        String primaryGroup = lpUser.getPrimaryGroup();
                        if (primaryGroup != null)
                            groups.add(primaryGroup.toLowerCase());
                        for (var node : lpUser.getNodes()) {
                            if (node.getKey().startsWith("group.")) {
                                groups.add(node.getKey().substring(6).toLowerCase());
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            // Eşlemedeki tüm rolleri kaldır (grup kontrolünden bağımsız - hesap
            // kaldırılıyor)
            for (String groupName : mappings.getKeys(false)) {
                String discordRoleId = mappings.getString(groupName);
                if (discordRoleId == null || discordRoleId.isEmpty()
                        || discordRoleId.equals("DISCORD_ROL_ID_BURAYA"))
                    continue;

                Role role = guild.getRoleById(discordRoleId);
                if (role == null)
                    continue;

                // Üyenin bu rolü var mı kontrol et
                if (member.getRoles().contains(role)) {
                    guild.removeRoleFromMember(member, role).queue(
                            success -> plugin.getLogger().info(
                                    MessageUtil.format("discord.luckperms-role-removed",
                                            Map.of("player", member.getEffectiveName(), "role", groupName))),
                            error -> plugin.getLogger().warning(
                                    MessageUtil.format("discord.role-fail",
                                            Map.of("error", error.getMessage()))));
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("⚠ LuckPerms rol kaldırma hatası: " + e.getMessage());
        }
    }

    /**
     * Log kanalına hesap kaldırma bilgisi gönderir.
     */
    private void sendUnlinkLog(String playerName) {
        if (plugin.getDiscordBot() == null)
            return;

        String description = MessageUtil.format("discord.log-unlinked",
                Map.of("player", playerName));

        plugin.getDiscordBot().sendLogEmbed("🔓 Hesap Bağlantısı Kaldırıldı", description, new Color(244, 67, 54));
    }
}