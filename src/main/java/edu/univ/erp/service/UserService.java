package edu.univ.erp.service;

import edu.univ.erp.db.DBManager;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserService {

    public static List<Map<String, Object>> listUsers() {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = "SELECT user_id, username, role, status, last_login FROM users_auth ORDER BY user_id";
        try (Connection conn = DBManager.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> r = new HashMap<>();
                r.put("user_id", rs.getInt("user_id"));
                r.put("username", rs.getString("username"));
                r.put("role", rs.getString("role"));
                r.put("status", rs.getString("status"));
                r.put("last_login", rs.getTimestamp("last_login"));
                out.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static boolean addUser(String username, String role, String plainPassword, String status) {
        String sql = "INSERT INTO users_auth (username, role, password_hash, status) VALUES (?, ?, ?, ?)";
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
        try (Connection conn = DBManager.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, role);
            ps.setString(3, hash);
            ps.setString(4, status);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateUser(Integer userId, String username, String role, String plainPassword, String status) {
        String sqlWithPass = "UPDATE users_auth SET username = ?, role = ?, password_hash = ?, status = ? WHERE user_id = ?";
        String sqlNoPass = "UPDATE users_auth SET username = ?, role = ?, status = ? WHERE user_id = ?";
        try (Connection conn = DBManager.getAuthConnection()) {
            if (plainPassword != null && !plainPassword.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(sqlWithPass)) {
                    ps.setString(1, username);
                    ps.setString(2, role);
                    ps.setString(3, BCrypt.hashpw(plainPassword, BCrypt.gensalt(10)));
                    ps.setString(4, status);
                    ps.setInt(5, userId);
                    return ps.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(sqlNoPass)) {
                    ps.setString(1, username);
                    ps.setString(2, role);
                    ps.setString(3, status);
                    ps.setInt(4, userId);
                    return ps.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteUser(int userId) {
        String sql = "DELETE FROM users_auth WHERE user_id = ?";
        try (Connection conn = DBManager.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int r = ps.executeUpdate();
            return r > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Map<String, Object> findById(int userId) {
        String sql = "SELECT user_id, username, role, status, last_login FROM users_auth WHERE user_id = ?";
        try (Connection conn = DBManager.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> r = new HashMap<>();
                    r.put("user_id", rs.getInt("user_id"));
                    r.put("username", rs.getString("username"));
                    r.put("role", rs.getString("role"));
                    r.put("status", rs.getString("status"));
                    r.put("last_login", rs.getTimestamp("last_login"));
                    return r;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}