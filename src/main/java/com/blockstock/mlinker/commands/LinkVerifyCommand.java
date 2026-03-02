package com.blockstock.mlinker.commands;

import java.awt.Color;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.blockstock.mlinker.MLinker;
import com.blockstock.mlinker.managers.LinkManager;
import com.blockstock.mlinker.managers.RewardManager;
import com.blockstock.mlinker.rewards.RewardLogManager;
import com.blockstock.mlinker.storage.StorageProvider;
import com.blockstock.mlinker.utils.MessageUtil;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

public class LinkVerifyCommand extends ListenerAdapter {

    private final MLinker plugin;

    public LinkVerifyCommand(MLinker plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        // Doğru komut değilse işlemi yoksay
        if (!event.getName().equalsIgnoreCase("hesapesle"))
            return;

        OptionMapping kodOption = event.getOption("kod");
        String code = kodOption != null ? kodOption.getAsString().trim() : null;
        if (code == null || code.isEmpty()) {
            sendEmbed(event,
                    MessageUtil.get("discord.code-missing-title"),
                    MessageUtil.get("discord.code-missing-desc"),
                    Color.RED);
            return;
        }

        LinkManager linkManager = plugin.getLinkManager();
        StorageProvider storage = plugin.getStorageProvider();

        // 1. Discord hesabı zaten bağlı mı?
        if (storage.isDiscordLinked(event.getUser().getId())) {
            sendEmbed(event,
                    MessageUtil.get("discord.already-linked-title"),
                    MessageUtil.get("discord.already-linked-desc"),
                    Color.RED);
            return;
        }

        // 2. Kod geçerli mi?
        if (!linkManager.isValidCode(code)) {
            sendEmbed(event,
                    MessageUtil.get("discord.invalid-title"),
                    MessageUtil.get("discord.invalid-desc"),
                    Color.RED);
            return;
        }

        UUID uuid = linkManager.getPlayerByCode(code);

        // 3. Minecraft hesabı zaten bağlı mı?
        if (storage.isPlayerLinked(uuid)) {
            sendEmbed(event,
                    MessageUtil.get("discord.player-already-linked-title"),
                    MessageUtil.get("discord.player-already-linked-desc"),
                    Color.RED);
            linkManager.removeCodeByUUID(uuid);
            return;
        }

        // Veritabanı kayıt işlemleri
        String playerName = Bukkit.getOfflinePlayer(uuid).getName();
        if (playerName == null)
            playerName = "Bilinmiyor";

        storage.setLinkedAccount(uuid, playerName, event.getUser().getId());
        linkManager.removeCodeByUUID(uuid);

        // Discord rolünü ve başarı mesajını gönder
        applyVerifiedRole(event);

        // LuckPerms rol eşlemesi
        applyLuckPermsRoles(event, uuid);

        sendEmbed(event,
                MessageUtil.format("discord.success-title", Map.of("player", playerName)),
                MessageUtil.format("discord.success-desc", Map.of("player", playerName)),
                Color.GREEN);

        plugin.getLogger().info(MessageUtil.format("discord.verified-console", Map.of("player", playerName)));

        // Log kanalına bilgilendirme gönder
        sendLinkLog(playerName, event.getUser().getName());

        // 4. BUKKIT API İŞLEMLERİ: Oyuncuya mesaj atma ve ödül verme işlemlerini ANA
        // THREAD'e taşıyoruz.
        final String finalPlayerName = playerName;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);

            // Oyuncu çevrimiçiyse ödülü ve mesajı ver
            if (player != null && player.isOnline()) {
                player.sendMessage(MessageUtil.format("link.success", Map.of("player", finalPlayerName)));

                RewardManager rewardManager = plugin.getRewardManager();
                RewardLogManager logManager = plugin.getRewardLogManager();

                if (rewardManager != null) {
                    rewardManager.grantFirstLink(player);

                    if (logManager != null) {
                        logManager.logReward(
                                player.getUniqueId(),
                                "first-link",
                                MessageUtil.format("reward-log.saved",
                                        Map.of("type", "first-link", "player", finalPlayerName)));
                    }
                }
            }
        });
    }

    private void applyVerifiedRole(SlashCommandInteractionEvent event) {
        String guildId = plugin.getConfig().getString("discord.guild-id");
        String roleId = plugin.getConfig().getString("discord.role-id-verified");

        if (guildId == null || roleId == null || guildId.isEmpty() || roleId.isEmpty()) {
            plugin.getLogger().warning(MessageUtil.get("discord.role-missing"));
            return;
        }

        Guild guild = event.getJDA().getGuildById(guildId);
        if (guild == null) {
            plugin.getLogger().warning(MessageUtil.format("discord.guild-missing", Map.of("guild", guildId)));
            return;
        }

        Role role = guild.getRoleById(roleId);
        if (role == null) {
            plugin.getLogger().warning(MessageUtil.format("discord.role-missing", Map.of("role", roleId)));
            return;
        }

        Member member = event.getMember();
        if (member == null)
            member = guild.retrieveMemberById(event.getUser().getId()).complete();
        if (member == null)
            return;

        final Member finalMember = member;
        guild.addRoleToMember(finalMember, role).queue(
                success -> plugin.getLogger().info(
                        MessageUtil.format("discord.role-success", Map.of("player", finalMember.getEffectiveName()))),
                error -> plugin.getLogger()
                        .warning(MessageUtil.format("discord.role-fail", Map.of("error", error.getMessage()))));
    }

    /**
     * LuckPerms grup → Discord rol eşlemesi.
     * Oyuncunun LuckPerms gruplarını kontrol eder ve config'deki eşlemeye göre
     * Discord rolleri verir.
     */
    private void applyLuckPermsRoles(SlashCommandInteractionEvent event, UUID playerUUID) {
        // LuckPerms rol eşlemesi etkin mi?
        if (!plugin.getConfig().getBoolean("discord.luckperms-roles.enabled", false))
            return;

        ConfigurationSection mappings = plugin.getConfig().getConfigurationSection("discord.luckperms-roles.mappings");
        if (mappings == null || mappings.getKeys(false).isEmpty())
            return;

        // LuckPerms API'sine erişim
        LuckPerms luckPerms;
        try {
            var provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider == null) {
                plugin.getLogger().warning(MessageUtil.get("discord.luckperms-not-found"));
                return;
            }
            luckPerms = provider.getProvider();
        } catch (Exception e) {
            plugin.getLogger().warning(MessageUtil.get("discord.luckperms-not-found"));
            return;
        }

        // Oyuncunun LuckPerms kullanıcısını al
        User lpUser = luckPerms.getUserManager().getUser(playerUUID);
        if (lpUser == null) {
            // Kullanıcı yüklenmemiş olabilir, yüklemeyi dene
            try {
                lpUser = luckPerms.getUserManager().loadUser(playerUUID).join();
            } catch (Exception e) {
                plugin.getLogger().warning("⚠ LuckPerms kullanıcısı yüklenemedi: " + e.getMessage());
                return;
            }
        }

        if (lpUser == null)
            return;

        // Oyuncunun tüm gruplarını al (inherited dahil)
        Set<String> groups = new java.util.HashSet<>();
        // Ana grup
        String primaryGroup = lpUser.getPrimaryGroup();
        if (primaryGroup != null)
            groups.add(primaryGroup.toLowerCase());

        // Tüm inherited node'ları kontrol et
        for (var node : lpUser.getNodes()) {
            if (node.getKey().startsWith("group.")) {
                groups.add(node.getKey().substring(6).toLowerCase());
            }
        }

        // Guild bilgisi
        String guildId = plugin.getConfig().getString("discord.guild-id");
        if (guildId == null || guildId.isEmpty())
            return;

        Guild guild = event.getJDA().getGuildById(guildId);
        if (guild == null)
            return;

        Member member = event.getMember();
        if (member == null)
            member = guild.retrieveMemberById(event.getUser().getId()).complete();
        if (member == null)
            return;

        final Member finalMember = member;

        // Her eşlemeyi kontrol et
        for (String groupName : mappings.getKeys(false)) {
            String discordRoleId = mappings.getString(groupName);
            if (discordRoleId == null || discordRoleId.isEmpty()
                    || discordRoleId.equals("DISCORD_ROL_ID_BURAYA"))
                continue;

            // Oyuncu bu gruba sahip mi?
            if (groups.contains(groupName.toLowerCase())) {
                Role role = guild.getRoleById(discordRoleId);
                if (role == null) {
                    plugin.getLogger()
                            .warning("⚠ Discord rolü bulunamadı: " + discordRoleId + " (grup: " + groupName + ")");
                    continue;
                }

                guild.addRoleToMember(finalMember, role).queue(
                        success -> plugin.getLogger().info(
                                MessageUtil.format("discord.luckperms-role-given",
                                        Map.of("player", finalMember.getEffectiveName(), "role", groupName))),
                        error -> plugin.getLogger().warning(
                                MessageUtil.format("discord.role-fail",
                                        Map.of("error", error.getMessage()))));
            }
        }
    }

    /**
     * Log kanalına hesap eşleme bilgisi gönderir.
     */
    private void sendLinkLog(String playerName, String discordTag) {
        if (plugin.getDiscordBot() == null)
            return;

        String description = MessageUtil.format("discord.log-linked",
                Map.of("player", playerName)) + "\n**Discord:** " + discordTag;

        plugin.getDiscordBot().sendLogEmbed("🔗 Hesap Eşleştirildi", description, new Color(0, 200, 83));
    }

    private void sendEmbed(SlashCommandInteractionEvent event, String title, String desc, Color color) {
        // Discord, Minecraft renk kodlarını algılamaz. Prefix'teki "&b" gibi kodları
        // temizleyelim.
        String rawPrefix = MessageUtil.get("decorations.prefix").replace("&", "§");
        String cleanPrefix = ChatColor.stripColor(rawPrefix).trim();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(desc)
                .setColor(color)
                .setFooter(cleanPrefix)
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}