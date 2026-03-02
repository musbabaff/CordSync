package com.blockstock.mlinker.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.blockstock.mlinker.MLinker;
import com.blockstock.mlinker.utils.MessageUtil;

public class MLinkerReloadCommand implements CommandExecutor {

    private final MLinker plugin;

    public MLinkerReloadCommand() {
        this.plugin = MLinker.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Yetki kontrolü (plugin.yml dosyasındaki yeni yetkiye göre uyarlandı)
        if (!sender.hasPermission("mlinker.admin")) {
            sender.sendMessage(MessageUtil.get("system.no-permission"));
            return true;
        }

        long start = System.currentTimeMillis();

        // Yapılandırma ve mesaj dosyalarını yeniden yükle
        plugin.reloadConfig();
        MessageUtil.load(plugin);

        // Not: İleride RewardManager (Ödül Sistemi) veya Discord Bot durumu
        // config'den tekrar çekilecekse buraya bir yenileme metodu ekleyebilirsin.
        // Örn: plugin.getRewardManager().reload();

        long took = System.currentTimeMillis() - start;

        // Konsol veya oyuncu sohbetinde renk kodunun bozulmaması için § kullanıldı
        sender.sendMessage(MessageUtil.get("system.reload") + " §7(" + took + "ms)");

        plugin.getLogger().info(MessageUtil.get("system.reload-detailed").replace("{time}", took + "ms"));

        return true;
    }
}