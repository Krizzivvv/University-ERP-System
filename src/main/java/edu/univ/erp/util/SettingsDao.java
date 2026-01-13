package edu.univ.erp.util;

import edu.univ.erp.db.DBManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsDao {

    public SettingsDao() {
    }

    public String get(String key) {
        String sql = "SELECT `value` FROM settings WHERE `key` = ?";
        try (Connection c = DBManager.getErpConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading settings key=" + key, e);
        }
    }

    public void set(String key, String value) {
        String sql = "INSERT INTO settings (`key`, `value`) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE `value` = ?";
        try (Connection c = DBManager.getErpConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error writing settings key=" + key, e);
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String v = get(key);
        if (v == null) return defaultValue;
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }
}
