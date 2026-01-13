package edu.univ.erp.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mindrot.jbcrypt.BCrypt;

import edu.univ.erp.db.DBManager;
import edu.univ.erp.model.AuthUser;

public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000; // 15 minutes
    private static final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private static final Map<String, Long> lockoutTime = new ConcurrentHashMap<>();

    public static AuthUser login(String username, String password) {
        // Check if user is locked out
        if (isLockedOut(username)) {
            long remainingMs = lockoutTime.get(username) + LOCKOUT_DURATION_MS - System.currentTimeMillis();
            long remainingMin = (remainingMs / 1000 / 60) + 1;
            System.out.println("[AuthService] Login denied: User '" + username + "' is locked out for " + remainingMin + " more minutes");
            return null;
        }

        String sql = "SELECT user_id, username, password_hash, role, status FROM users_auth WHERE username=? LIMIT 1";

        try (Connection conn = DBManager.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    recordFailedAttempt(username);
                    return null;
                }

                // Check if user is active
                String status = rs.getString("status");
                if (status == null || !status.equalsIgnoreCase("active")) {
                    System.out.println("[AuthService] Login denied: User '" + username + "' is disabled (status: " + status + ")");
                    return null;
                }

                String dbHash = rs.getString("password_hash");

                boolean ok;
                if (dbHash.startsWith("$2")) {
                    ok = BCrypt.checkpw(password, dbHash);
                } else {
                    ok = password.equals(dbHash);
                }

                if (!ok) {
                    recordFailedAttempt(username);
                    return null;
                }

                clearFailedAttempts(username);
                updateLastLogin(rs.getInt("user_id"));

                return new AuthUser(rs.getInt("user_id"), username, rs.getString("role"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private static boolean isLockedOut(String username) {
        Long lockedAt = lockoutTime.get(username);
        if (lockedAt == null) return false;
        
        if (System.currentTimeMillis() - lockedAt > LOCKOUT_DURATION_MS) {
            // Lockout expired
            lockoutTime.remove(username);
            failedAttempts.remove(username);
            return false;
        }
        return true;
    }

    private static void recordFailedAttempt(String username) {
        int attempts = failedAttempts.getOrDefault(username, 0) + 1;
        failedAttempts.put(username, attempts);
        
        System.out.println("[AuthService] Failed login attempt " + attempts + " for user: " + username);
        
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            lockoutTime.put(username, System.currentTimeMillis());
            System.out.println("[AuthService] User '" + username + "' locked out after " + MAX_FAILED_ATTEMPTS + " failed attempts");
        }
    }

    private static void clearFailedAttempts(String username) {
        failedAttempts.remove(username);
        lockoutTime.remove(username);
    }

    public static int getRemainingAttempts(String username) {
        if (isLockedOut(username)) return -1;
        int attempts = failedAttempts.getOrDefault(username, 0);
        return MAX_FAILED_ATTEMPTS - attempts;
    }

    public static long getLockoutRemainingMinutes(String username) {
        if (!isLockedOut(username)) return 0;
        long remainingMs = lockoutTime.get(username) + LOCKOUT_DURATION_MS - System.currentTimeMillis();
        return (remainingMs / 1000 / 60) + 1;
    }

    private static void updateLastLogin(int userId) {
        String sql = "UPDATE users_auth SET last_login = NOW() WHERE user_id = ?";
        
        try (Connection conn = DBManager.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("[WARN] Failed to update last_login for user_id: " + userId);
        }
    }

    // registerStudent method
    public boolean registerStudent(String username, String role, String password,
                                String rollNo, String program, int year) {

        if (username == null || username.isBlank() ||
            password == null || password.isBlank()) {
            return false;
        }

        if (role == null || role.isBlank()) {
            role = "student";
        }

        String insertAuth = "INSERT INTO users_auth (username, role, password_hash, status) VALUES (?, ?, ?, 'active')";
        String insertStudent = "INSERT INTO students (user_id, roll_no, program, year) VALUES (?, ?, ?, ?)";

        Connection authConn = null;
        PreparedStatement psAuth = null;

        long userId = -1;

        try {
            // 1) Insert into auth DB
            authConn = DBManager.getAuthConnection();
            authConn.setAutoCommit(false);

            psAuth = authConn.prepareStatement(insertAuth, java.sql.Statement.RETURN_GENERATED_KEYS);
            psAuth.setString(1, username);
            psAuth.setString(2, role);
            psAuth.setString(3, BCrypt.hashpw(password, BCrypt.gensalt(10)));

            int r = psAuth.executeUpdate();
            if (r != 1) {
                authConn.rollback();
                return false;
            }

            try (ResultSet keys = psAuth.getGeneratedKeys()) {
                if (keys.next()) {
                    userId = keys.getLong(1);
                }
            }

            if (userId == -1) {
                authConn.rollback();
                return false;
            }

            // commit immediately so lock is released
            authConn.commit();
            authConn.setAutoCommit(true);

        } catch (Exception e) {
            e.printStackTrace();
            try { if (authConn != null) authConn.rollback(); } catch (Exception ignore) {}
            try { if (authConn != null) authConn.setAutoCommit(true); } catch (Exception ignore) {}
            return false;
        } finally {
            try { if (psAuth != null) psAuth.close(); } catch (Exception ignore) {}
            try { if (authConn != null) authConn.close(); } catch (Exception ignore) {}
        }

        // 2) Insert into students table using a NEW short-lived connection
        try (Connection erpConn = DBManager.getErpConnection();
            PreparedStatement psStu = erpConn.prepareStatement(insertStudent)) {

            psStu.setLong(1, userId);
            psStu.setString(2, rollNo);
            psStu.setString(3, program);
            psStu.setInt(4, year);

            psStu.executeUpdate();
            return true;

        } catch (SQLException ex) {
            ex.printStackTrace();

            // CLEANUP
            try (Connection cleanupConn = DBManager.getAuthConnection();
                PreparedStatement del = cleanupConn.prepareStatement(
                        "DELETE FROM users_auth WHERE user_id = ?")) {

                del.setLong(1, userId);
                del.executeUpdate();

                System.err.println("Removed orphan auth row id=" + userId);

            } catch (Exception cleanupEx) {
                cleanupEx.printStackTrace();
            }

            return false;
        }
    }

    public static boolean setUserStatus(int userId, boolean active) {
        String sql = "UPDATE users_auth SET status = ? WHERE user_id = ?";
        
        try (Connection conn = DBManager.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, active ? "active" : "disabled");
            ps.setInt(2, userId);
            
            int rows = ps.executeUpdate();
            
            if (rows > 0) {
                System.out.println("[AuthService] User status updated: user_id=" + userId + " status=" + (active ? "active" : "disabled"));
                return true;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("[ERROR] Failed to update user status for user_id: " + userId);
        }
        
        return false;
    }

    public static boolean isUserActive(int userId) {
        String sql = "SELECT status FROM users_auth WHERE user_id = ?";
        
        try (Connection conn = DBManager.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    return status != null && status.equalsIgnoreCase("active");
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
}