package com.blockstock.mlinker.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.blockstock.mlinker.MLinker;

public class MLinkerInfoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MLinker plugin = MLinker.getInstance();

        sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§b§lmLinker §7v" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Geliştirici: §fmusbabaff");
        sender.sendMessage("");
        sender.sendMessage("§7Depolama: §f" + plugin.getConfig().getString("storage.type", "Bilinmiyor"));
        sender.sendMessage("§7Dil: §f" + plugin.getConfig().getString("language", "TR"));
        sender.sendMessage("§7ReVerify: §f" + (plugin.getReverifyTask() != null ? "§aAktif" : "§cPasif"));
        sender.sendMessage("§7Discord Botu: §f" + (plugin.getDiscordBot() != null ? "§aBağlı" : "§cKapalı"));
        sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return true;
    }
}