package com.blockstock.mlinker.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.blockstock.mlinker.MLinker;
import com.blockstock.mlinker.managers.LinkManager;
import com.blockstock.mlinker.storage.StorageProvider;
import com.blockstock.mlinker.utils.MessageUtil;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class LinkCommand implements CommandExecutor {

    private final LinkManager linkManager;
    private final MLinker plugin;
    private final StorageProvider storage;

    public LinkCommand(LinkManager linkManager) {
        this.linkManager = linkManager;
        this.plugin = MLinker.getInstance();
        this.storage = plugin.getStorageProvider();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Konsol kontrolü
        if (!(sender instanceof Player)) {
            // MessageUtil zaten renkleri çeviriyorsa ekstra ChatColor kullanmaya gerek yok
            sender.sendMessage(MessageUtil.get("link.not-player"));
            return true;
        }

        Player player = (Player) sender;

        // Oyuncu zaten eşleştirilmiş mi?
        if (storage.isPlayerLinked(player.getUniqueId())) {
            player.sendMessage(MessageUtil.get("link.already-linked"));
            return true;
        }

        // Oyuncunun halihazırda süresi dolmamış aktif bir kodu var mı?
        String existing = linkManager.getCode(player);
        if (existing != null) {
            sendStyledCodeMessage(player, existing, true);
            return true;
        }

        // Yeni kod oluştur
        String newCode = linkManager.generateCode(player);
        sendStyledCodeMessage(player, newCode, false);

        return true;
    }

    private void sendStyledCodeMessage(Player player, String code, boolean isExisting) {

        player.sendMessage("");
        player.sendMessage(MessageUtil.get("link.header"));
        player.sendMessage(MessageUtil.get("link.title"));

        if (isExisting) {
            player.sendMessage(MessageUtil.get("link.code-active"));
        } else {
            player.sendMessage(MessageUtil.get("link.code-new"));
        }

        // Tıklanabilir şık metin tasarımı
        TextComponent clickableCode = new TextComponent("「 " + code + " 」");
        clickableCode.setColor(ChatColor.AQUA);
        clickableCode.setBold(true);

        // ÖNEMLİ DEĞİŞİKLİK: 1.15+ için COPY_TO_CLIPBOARD kullanarak oyuncunun kod
        // kopyalamasını kolaylaştırdık.
        clickableCode.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code));

        clickableCode.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(MessageUtil.get("link.copied-hover"))));

        // Spigot API ile Bungee chat mesajını gönder
        player.spigot().sendMessage(clickableCode);

        // Kullanım talimatını gönder
        player.sendMessage(MessageUtil.get("link.usage").replace("<kod>", code));
        player.sendMessage(MessageUtil.get("link.header"));
        player.sendMessage("");
    }
}