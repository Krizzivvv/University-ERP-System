package edu.univ.erp.service;

import edu.univ.erp.db.DBManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

public class SectionService {

    public static List<Map<String, Object>> listInstructors() {
        List<Map<String,Object>> out = new ArrayList<>();
        
        String ensureSql = "INSERT INTO instructors (user_id, department) " +
                        "SELECT ua.user_id, 'General' " +
                        "FROM univ_auth.users_auth ua " +
                        "WHERE ua.role = 'instructor' AND ua.status = 'active' " +
                        "AND NOT EXISTS (SELECT 1 FROM instructors i WHERE i.user_id = ua.user_id)";
        
        try (Connection conn = DBManager.getErpConnection();
            PreparedStatement ps = conn.prepareStatement(ensureSql)) {
            int created = ps.executeUpdate();
            if (created > 0) {
                System.out.println("[SectionService] Auto-created " + created + " missing instructor entries");
            }
        } catch (SQLException ex) {
            System.err.println("[SectionService] Warning: Could not auto-create instructor entries: " + ex.getMessage());
        }
        
        // Now fetch all instructors
        String sql = "SELECT ua.user_id, ua.username, COALESCE(i.department, 'General') as department " +
                    "FROM univ_auth.users_auth ua " +
                    "INNER JOIN instructors i ON i.user_id = ua.user_id " +
                    "WHERE ua.role = 'instructor' AND ua.status = 'active' " +
                    "ORDER BY ua.username";
        
        try (Connection conn = DBManager.getErpConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String,Object> r = new HashMap<>();
                r.put("user_id", rs.getInt("user_id"));
                r.put("username", rs.getString("username"));
                r.put("department", rs.getString("department"));
                out.add(r);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
        return out;
    }

    public static boolean createSection(int courseId, Integer instructorId, String dayTime, String room,
                                        int capacity, String semester, int year) {
        String sql = "INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            if (instructorId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, instructorId);
            ps.setString(3, dayTime);
            ps.setString(4, room);
            ps.setInt(5, capacity);
            ps.setString(6, semester);
            ps.setInt(7, year);
            int affected = ps.executeUpdate();
            System.out.println("createSection affected=" + affected + " instructorId=" + instructorId);
            return affected > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static List<Map<String, Object>> listSectionsForCourse(int courseId) {
        List<Map<String,Object>> out = new ArrayList<>();
        String sql = "SELECT s.section_id, s.course_id, s.instructor_id, s.day_time, s.room, s.capacity, s.semester, s.year, " +
                     "       ua.username AS instructor_username, " +
                     "       (SELECT COUNT(*) FROM enrollments e WHERE e.section_id = s.section_id AND e.status='enrolled') AS enrolled_count " +
                     "FROM sections s " +
                     "LEFT JOIN univ_auth.users_auth ua ON ua.user_id = s.instructor_id " +
                     "WHERE s.course_id = ? " +
                     "ORDER BY s.section_id";
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> r = new HashMap<>();
                    r.put("section_id", rs.getInt("section_id"));
                    r.put("course_id", rs.getInt("course_id"));
                    r.put("instructor_id", rs.getObject("instructor_id"));
                    r.put("day_time", rs.getString("day_time"));
                    r.put("room", rs.getString("room"));
                    r.put("capacity", rs.getInt("capacity"));
                    r.put("semester", rs.getString("semester"));
                    r.put("year", rs.getInt("year"));
                    r.put("instructor_username", rs.getString("instructor_username"));
                    r.put("enrolled_count", rs.getInt("enrolled_count"));
                    out.add(r);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return out;
    }

    public static Map<String, Object> findSectionById(int sectionId) {
        String sql = "SELECT s.section_id, s.course_id, s.instructor_id, s.day_time, s.room, s.capacity, s.semester, s.year, " +
                     "       ua.username AS instructor_username " +
                     "FROM sections s " +
                     "LEFT JOIN univ_auth.users_auth ua ON ua.user_id = s.instructor_id " +
                     "WHERE s.section_id = ?";
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String,Object> r = new HashMap<>();
                    r.put("section_id", rs.getInt("section_id"));
                    r.put("course_id", rs.getInt("course_id"));
                    r.put("instructor_id", rs.getObject("instructor_id"));
                    r.put("day_time", rs.getString("day_time"));
                    r.put("room", rs.getString("room"));
                    r.put("capacity", rs.getInt("capacity"));
                    r.put("semester", rs.getString("semester"));
                    r.put("year", rs.getInt("year"));
                    r.put("instructor_username", rs.getString("instructor_username"));
                    return r;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static boolean updateSection(int sectionId, Integer instructorId, String dayTime, String room,
                                        int capacity, String semester, int year) {
        String sql = "UPDATE sections SET instructor_id = ?, day_time = ?, room = ?, capacity = ?, semester = ?, year = ? WHERE section_id = ?";
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (instructorId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, instructorId);
            ps.setString(2, dayTime);
            ps.setString(3, room);
            ps.setInt(4, capacity);
            ps.setString(5, semester);
            ps.setInt(6, year);
            ps.setInt(7, sectionId);
            int affected = ps.executeUpdate();
            System.out.println("updateSection affected=" + affected + " instr=" + instructorId + " sec=" + sectionId);
            return affected > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static List<Map<String,Object>> listSectionsForInstructor(int instructorId) {
        List<Map<String,Object>> out = new ArrayList<>();
        String sql = ""
            + "SELECT s.section_id, c.code AS course_code, c.title AS course_title, s.day_time, s.room, s.capacity, "
            + "       IFNULL(e.enrolled, 0) AS enrolled "
            + "FROM sections s "
            + "JOIN courses c ON s.course_id = c.course_id "
            + "LEFT JOIN (SELECT section_id, COUNT(*) AS enrolled FROM enrollments WHERE status = 'enrolled' GROUP BY section_id) e "
            + "  ON e.section_id = s.section_id "
            + "WHERE s.instructor_id = ? "
            + "ORDER BY s.section_id";
        try (Connection c = DBManager.getErpConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> m = new HashMap<>();
                    m.put("section_id", rs.getInt("section_id"));
                    m.put("course_code", rs.getString("course_code"));
                    m.put("course_title", rs.getString("course_title"));
                    m.put("day_time", rs.getString("day_time"));
                    m.put("room", rs.getString("room"));
                    m.put("capacity", rs.getInt("capacity"));
                    m.put("enrolled", rs.getInt("enrolled"));
                    out.add(m);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return out;
    }

    public static List<Map<String,Object>> listEnrollmentsForSection(int sectionId) {
        List<Map<String,Object>> out = new ArrayList<>();
        String sql = ""
            + "SELECT e.enrollment_id, ua.user_id, ua.username, s.roll_no "
            + "FROM enrollments e "
            + "JOIN univ_auth.users_auth ua ON ua.user_id = e.student_id "
            + "LEFT JOIN students s ON s.user_id = ua.user_id "
            + "WHERE e.section_id = ? AND e.status = 'enrolled' "
            + "ORDER BY ua.username";
        try (Connection c = DBManager.getErpConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> m = new HashMap<>();
                    m.put("enrollment_id", rs.getInt("enrollment_id"));
                    m.put("user_id", rs.getInt("user_id"));
                    m.put("username", rs.getString("username"));
                    m.put("roll_no", rs.getString("roll_no"));
                    out.add(m);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return out;
    }

    public static boolean isInstructorAssignedToSection(int instructorUserId, int sectionId) {
        String sql = "SELECT 1 FROM sections WHERE section_id = ? AND instructor_id = ?";
        try (Connection conn = DBManager.getErpConnection(); // Use your actual connection manager
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sectionId);
            ps.setInt(2, instructorUserId);

            try (ResultSet rs = ps.executeQuery()) {
                boolean isAssigned = rs.next();
                return isAssigned;
            }
        } catch (SQLException e) {
            Logger.getLogger(SectionService.class.getName()).log(Level.SEVERE, "Error checking instructor assignment for section " + sectionId + " and instructor " + instructorUserId, e);
            return false;
        }
    }

    public static int getSectionIdForEnrollment(int enrollmentId) throws SQLException {
        String sql = "SELECT section_id FROM enrollments WHERE enrollment_id = ?";
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("section_id");
                } else {
                    // Handle case where enrollment ID doesn't exist
                    throw new SQLException("Enrollment ID not found: " + enrollmentId);
                }
            }
        }
    }

    public static Timestamp[] getRegistrationAndDropWindow(int sectionId) {
        String sql = "SELECT registration_start, registration_end, drop_deadline FROM sections WHERE section_id = ?";
        try (Connection conn = DBManager.getErpConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Timestamp[] {
                        rs.getTimestamp("registration_start"),
                        rs.getTimestamp("registration_end"),
                        rs.getTimestamp("drop_deadline")
                    };
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return new Timestamp[] { null, null, null };
    }

    public static boolean updateRegistrationAndDropWindow(int sectionId, Timestamp regStart, Timestamp regEnd, Timestamp dropDeadline) {
        String sql = "UPDATE sections SET registration_start = ?, registration_end = ?, drop_deadline = ? WHERE section_id = ?";
        try (Connection conn = DBManager.getErpConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, regStart);
            ps.setTimestamp(2, regEnd);
            ps.setTimestamp(3, dropDeadline);
            ps.setInt(4, sectionId);
            int updated = ps.executeUpdate();
            return updated == 1;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}