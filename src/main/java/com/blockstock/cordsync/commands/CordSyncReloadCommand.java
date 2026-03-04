package com.blockstock.cordsync.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.blockstock.cordsync.CordSync;
import com.blockstock.cordsync.utils.MessageUtil;

public class CordSyncReloadCommand implements CommandExecutor {

    private final CordSync plugin;

    public CordSyncReloadCommand() {
        this.plugin = CordSync.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Yetki kontrolÃ¼ (plugin.yml dosyasÄ±ndaki yeni yetkiye gÃ¶re uyarlandÄ±)
        if (!sender.hasPermission("CordSync.admin")) {
            sender.sendMessage(MessageUtil.get("system.no-permission"));
            return true;
        }

        long start = System.currentTimeMillis();

        // YapÄ±landÄ±rma ve mesaj dosyalarÄ±nÄ± yeniden yÃ¼kle
        plugin.reloadConfig();
        MessageUtil.load(plugin);

        // Not: Ä°leride RewardManager (Ã–dÃ¼l Sistemi) veya Discord Bot durumu
        // config'den tekrar Ã§ekilecekse buraya bir yenileme metodu ekleyebilirsin.
        // Ã–rn: plugin.getRewardManager().reload();

        long took = System.currentTimeMillis() - start;

        // Konsol veya oyuncu sohbetinde renk kodunun bozulmamasÄ± iÃ§in Â§ kullanÄ±ldÄ±
        sender.sendMessage(MessageUtil.get("system.reload") + " Â§7(" + took + "ms)");

        plugin.getLogger().info(MessageUtil.get("system.reload-detailed").replace("{time}", took + "ms"));

        return true;
    }
}

