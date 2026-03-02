package com.blockstock.mlinker.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.blockstock.mlinker.MLinker;

public class MySQLStorage implements StorageProvider, Migratable {

    private final MLinker plugin;
    private Connection connection;

    public MySQLStorage(MLinker plugin) {
        this.plugin = plugin;
        connect();
        createTable();
    }

    private void connect() {
        String host = plugin.getConfig().getString("storage.mysql.host");
        int port = plugin.getConfig().getInt("storage.mysql.port");
        String database = plugin.getConfig().getString("storage.mysql.database");
        String username = plugin.getConfig().getString("storage.mysql.username");
        String password = plugin.getConfig().getString("storage.mysql.password");
        try {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
            connection = DriverManager.getConnection(url, username, password);
            plugin.getLogger().info("✅ MySQL bağlantısı kuruldu: " + database);
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ MySQL bağlantısı başarısız: " + e.getMessage());
        }
    }

    private void createTable() {
        String sql =
                "CREATE TABLE IF NOT EXISTS linked_accounts (" +
                " uuid VARCHAR(36) PRIMARY KEY," +
                " username VARCHAR(64) NOT NULL," +
                " discord_id VARCHAR(32) NOT NULL UNIQUE" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ MySQL tablo oluşturulamadı: " + e.getMessage());
        }
    }

    @Override
    public void setLinkedAccount(UUID uuid, String playerName, String discordId) {
        String sql =
                "INSERT INTO linked_accounts (uuid, username, discord_id) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "username = VALUES(username), discord_id = VALUES(discord_id);";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, playerName);
            ps.setString(3, discordId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ MySQL kayıt işlemi başarısız: " + e.getMessage());
        }
    }

    @Override
    public String getDiscordId(UUID uuid) {
        String sql = "SELECT discord_id FROM linked_accounts WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("discord_id");
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Discord ID alınamadı: " + e.getMessage());
        }
        return null;
    }

    @Override
    public UUID getPlayerUUID(String discordId) {
        String sql = "SELECT uuid FROM linked_accounts WHERE discord_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, discordId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return UUID.fromString(rs.getString("uuid"));
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ UUID alınamadı: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void removeLinkedAccount(UUID uuid) {
        String sql = "DELETE FROM linked_accounts WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ MySQL bağlantı silinemedi: " + e.getMessage());
        }
    }

    @Override
    public boolean isPlayerLinked(UUID uuid) {
        String sql = "SELECT 1 FROM linked_accounts WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Oyuncu bağlantı kontrolü başarısız: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean isDiscordLinked(String discordId) {
        String sql = "SELECT 1 FROM linked_accounts WHERE discord_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, discordId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Discord bağlantı kontrolü başarısız: " + e.getMessage());
        }
        return false;
    }

    @Override
    public Map<UUID, Migratable.LinkedData> loadAllLinkedAccounts() {
        Map<UUID, Migratable.LinkedData> map = new HashMap<>();
        String sql = "SELECT uuid, username, discord_id FROM linked_accounts";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String username = rs.getString("username");
                String discordId = rs.getString("discord_id");
                map.put(uuid, new Migratable.LinkedData(username, discordId));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Kayıtlar okunamadı (MySQL): " + e.getMessage());
        }
        return map;
    }

    @Override
    public void importLinkedAccounts(Map<UUID, Migratable.LinkedData> accounts) {
        String sql =
                "INSERT INTO linked_accounts (uuid, username, discord_id) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "username = VALUES(username), discord_id = VALUES(discord_id);";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (Map.Entry<UUID, Migratable.LinkedData> e : accounts.entrySet()) {
                ps.setString(1, e.getKey().toString());
                ps.setString(2, e.getValue().playerName);
                ps.setString(3, e.getValue().discordId);
                ps.addBatch();
            }
            ps.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            try { connection.rollback(); } catch (Exception ignored) {}
            plugin.getLogger().severe("❌ Kayıtlar içe aktarılamadı (MySQL): " + e.getMessage());
        }
    }

    public Set<UUID> getAllLinkedPlayers() {
        Set<UUID> uuids = new HashSet<>();
        String sql = "SELECT uuid FROM linked_accounts";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                uuids.add(UUID.fromString(rs.getString("uuid")));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Oyuncu listesi alınamadı (MySQL): " + e.getMessage());
        }
        return uuids;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("💾 MySQL bağlantısı kapatıldı.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ MySQL bağlantısı kapatılamadı: " + e.getMessage());
        }
    }
}
