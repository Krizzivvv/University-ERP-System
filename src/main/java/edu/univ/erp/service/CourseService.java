package edu.univ.erp.service;

import edu.univ.erp.db.DBManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CourseService {

    public static List<Map<String, Object>> listCourses() {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = "SELECT course_id, code, title, credits FROM courses ORDER BY code";
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("course_id", rs.getInt("course_id"));
                row.put("code", rs.getString("code"));
                row.put("title", rs.getString("title"));
                row.put("credits", rs.getInt("credits"));
                out.add(row);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return out;
    }

    public static boolean addCourse(String code, String title, int credits) {
        String sql = "INSERT INTO courses (code, title, credits) VALUES (?, ?, ?)";
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, title);
            ps.setInt(3, credits);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean updateCourse(int courseId, String code, String title, int credits) {
        String sql = "UPDATE courses SET code = ?, title = ?, credits = ? WHERE course_id = ?";
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, title);
            ps.setInt(3, credits);
            ps.setInt(4, courseId);
            int r = ps.executeUpdate();
            return r > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean deleteCourse(int courseId) {
        String sql = "DELETE FROM courses WHERE course_id = ?";
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            int r = ps.executeUpdate();
            return r > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static Map<String, Object> findById(int courseId) {
        String sql = "SELECT course_id, code, title, credits FROM courses WHERE course_id = ?";
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("course_id", rs.getInt("course_id"));
                    row.put("code", rs.getString("code"));
                    row.put("title", rs.getString("title"));
                    row.put("credits", rs.getInt("credits"));
                    return row;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static List<Map<String, Object>> listCoursesWithEnrollmentCount() {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql =
            "SELECT c.course_id, c.code, c.title, c.credits, " +
            "       (SELECT COUNT(*) " +
            "        FROM enrollments e JOIN sections s ON e.section_id = s.section_id " +
            "        WHERE s.course_id = c.course_id AND e.status = 'enrolled') AS enrolled, " +
            "       (SELECT GROUP_CONCAT(DISTINCT ua.username ORDER BY ua.username SEPARATOR ', ') " +
            "        FROM sections s2 " +
            "        LEFT JOIN univ_auth.users_auth ua ON ua.user_id = s2.instructor_id " +
            "        WHERE s2.course_id = c.course_id AND s2.instructor_id IS NOT NULL) AS instructors " +
            "FROM courses c " +
            "ORDER BY c.code";
        try (Connection conn = DBManager.getErpConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String,Object> row = new HashMap<>();
                row.put("course_id", rs.getInt("course_id"));
                row.put("code", rs.getString("code"));
                row.put("title", rs.getString("title"));
                row.put("credits", rs.getInt("credits"));
                row.put("enrolled", rs.getInt("enrolled"));
                row.put("instructors", rs.getString("instructors"));
                out.add(row);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return out;
    }

    public static Map<String, Object> findByCode(String code) {
        String sql = "SELECT course_id, code, title, credits FROM courses WHERE code = ?";
        try (Connection conn = DBManager.getErpConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("course_id", rs.getInt("course_id"));
                    row.put("code", rs.getString("code"));
                    row.put("title", rs.getString("title"));
                    row.put("credits", rs.getInt("credits"));
                    return row;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}