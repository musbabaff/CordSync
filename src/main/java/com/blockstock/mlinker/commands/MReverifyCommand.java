package com.blockstock.mlinker.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.blockstock.mlinker.MLinker;
import com.blockstock.mlinker.tasks.ReverifyTask;
import com.blockstock.mlinker.utils.MessageUtil;

public class MReverifyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Yetki kontrolü (plugin.yml dosyasında tanımladığımız yetki)
        if (!sender.hasPermission("mlinker.reverify") && !sender.hasPermission("mlinker.admin")) {
            sender.sendMessage(MessageUtil.get("system.no-permission"));
            return true;
        }

        try {
            ReverifyTask task = MLinker.getInstance().getReverifyTask();

            // tr.yml / en.yml dosyasından başlangıç mesajını gönderiyoruz
            sender.sendMessage(MessageUtil.get("reverify.start"));

            if (task != null) {
                // Görev zaten aktifse hemen çalıştır
                task.executeNow();
            } else {
                // Görev config üzerinden kapalıysa (null ise), geçici bir tane oluşturup sadece 1 kez çalıştır
                ReverifyTask newTask = new ReverifyTask(MLinker.getInstance());
                newTask.executeNow();
                sender.sendMessage("§a♻ Yeniden doğrulama süreci tek seferlik oluşturuldu ve başlatıldı.");
            }

        } catch (Exception e) {
            // Olası bir API veya veritabanı hatasında konsola/oyuncuya bilgi ver
            sender.sendMessage("§c❌ Yeniden doğrulama başlatılamadı: " + e.getMessage());
            MLinker.getInstance().getLogger().severe("ReVerify Command Error: " + e.getMessage());
        }

        return true;
    }
}