package edu.univ.erp.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.univ.erp.db.DBManager;
import edu.univ.erp.util.MaintenanceChecker;

public class StudentService {

    private static final MaintenanceChecker maintenanceChecker = new MaintenanceChecker();

    public static String enrollStudentSafe(int studentUserId, int sectionId) {
        if (maintenanceChecker.isMaintenanceOn()) {
            return "MAINTENANCE";
        }

        Connection conn = null;
        try {
            conn = DBManager.getErpConnection();
            conn.setAutoCommit(false);

            // Lock the section row and get capacity, registration window, and current enrolled count
            String lockSql =
                "SELECT capacity, " +
                " (SELECT COUNT(*) FROM enrollments e WHERE e.section_id = s.section_id AND e.status='enrolled') AS enrolled, " +
                " s.registration_start, s.registration_end " +
                "FROM sections s WHERE s.section_id = ? FOR UPDATE";

            int capacity = -1;
            int enrolled = -1;
            java.sql.Timestamp regStartTs = null;
            java.sql.Timestamp regEndTs = null;

            try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                ps.setInt(1, sectionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return "NOT_FOUND";
                    }
                    capacity = rs.getInt("capacity");
                    enrolled = rs.getInt("enrolled");
                    regStartTs = rs.getTimestamp("registration_start"); // may be null
                    regEndTs   = rs.getTimestamp("registration_end");   // may be null
                }
            }

            // Check registration window (server time)
            java.time.Instant now = java.time.Instant.now();
            if (regStartTs != null) {
                java.time.Instant startInstant = regStartTs.toInstant();
                if (now.isBefore(startInstant)) {
                    conn.rollback();
                    return "NOT_OPEN_YET";
                }
            }
            if (regEndTs != null) {
                java.time.Instant endInstant = regEndTs.toInstant();
                if (now.isAfter(endInstant)) {
                    conn.rollback();
                    return "REGISTRATION_CLOSED";
                }
            }

            // Check existing enrollment row for this student+section (could be enrolled OR dropped)
            final String findSql = "SELECT enrollment_id, status FROM enrollments WHERE student_id = ? AND section_id = ? LIMIT 1";
            Integer existingEnrollmentId = null;
            String existingStatus = null;
            try (PreparedStatement ps = conn.prepareStatement(findSql)) {
                ps.setInt(1, studentUserId);
                ps.setInt(2, sectionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        existingEnrollmentId = rs.getInt("enrollment_id");
                        existingStatus = rs.getString("status");
                    }
                }
            }

            if ("enrolled".equalsIgnoreCase(existingStatus)) {
                conn.rollback();
                return "DUPLICATE";
            }

            if (capacity > -1 && enrolled >= capacity) {
                // If there's a dropped row that belongs to this student, reactivation would still need a free slot.
                conn.rollback();
                return "FULL";
            }

            // Now either reactivate a dropped row or insert a new one
            java.sql.Timestamp nowTs = new java.sql.Timestamp(System.currentTimeMillis());

            if (existingEnrollmentId != null && "dropped".equalsIgnoreCase(existingStatus)) {
                final String reactivateSql = "UPDATE enrollments SET status = 'enrolled', dropped_at = NULL, enrolled_at = ? WHERE enrollment_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(reactivateSql)) {
                    ps.setTimestamp(1, nowTs);
                    ps.setInt(2, existingEnrollmentId);
                    int updated = ps.executeUpdate();
                    if (updated != 1) {
                        conn.rollback();
                        return "ERROR";
                    }
                }
                conn.commit();
                return "OK";
            } else {
                final String insertSql =
                    "INSERT INTO enrollments (student_id, section_id, status, created_at, enrolled_at) " +
                    "VALUES (?, ?, 'enrolled', NOW(), ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, studentUserId);
                    ps.setInt(2, sectionId);
                    ps.setTimestamp(3, nowTs);
                    int inserted = ps.executeUpdate();
                    if (inserted <= 0) {
                        conn.rollback();
                        return "ERROR";
                    }
                }
                conn.commit();
                return "OK";
            }

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ignore) {}
            if (e.getErrorCode() == 1062) {
                return "DUPLICATE";
            }
            e.printStackTrace();
            return "ERROR";
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignore) {}
            }
        }
    }



    public static boolean enrollStudent(int studentUserId, int sectionId) {
        String result = enrollStudentSafe(studentUserId, sectionId);
        return result.equals("OK");
    }

    public static boolean dropEnrollment(int studentUserId, int sectionId) {
        String result = dropEnrollmentSafe(studentUserId, sectionId);
        return result.equals("OK");
    }

    public static String registerForSection(int studentId, int sectionId) {
        return enrollStudentSafe(studentId, sectionId);
    }

    public static List<Map<String, Object>> listStudentsEnrolledInCourse(int courseOrSectionId) {
        List<Map<String,Object>> out = new ArrayList<>();

        String sqlSection =
            "SELECT ua.user_id, ua.username, s.roll_no, s.program, e.enrollment_id, sec.section_id, sec.day_time " +
            "FROM enrollments e " +
            "JOIN sections sec ON e.section_id = sec.section_id " +
            "JOIN univ_auth.users_auth ua ON ua.user_id = e.student_id " +
            "LEFT JOIN students s ON s.user_id = ua.user_id " +
            "WHERE sec.section_id = ? AND e.status = 'enrolled' " +
            "ORDER BY ua.username";

        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSection)) {

            ps.setInt(1, courseOrSectionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> r = new HashMap<>();
                    r.put("user_id", rs.getInt("user_id"));
                    r.put("username", rs.getString("username"));
                    r.put("roll_no", rs.getString("roll_no"));
                    r.put("program", rs.getString("program"));
                    r.put("enrollment_id", rs.getInt("enrollment_id"));
                    r.put("section_id", rs.getInt("section_id"));
                    r.put("day_time", rs.getString("day_time"));
                    out.add(r);
                }
            }

            if (out.isEmpty()) {
                String sqlCourse =
                    "SELECT ua.user_id, ua.username, s.roll_no, s.program, e.enrollment_id, sec.section_id, sec.day_time " +
                    "FROM enrollments e " +
                    "JOIN sections sec ON e.section_id = sec.section_id " +
                    "JOIN courses c ON sec.course_id = c.course_id " +
                    "JOIN univ_auth.users_auth ua ON ua.user_id = e.student_id " +
                    "LEFT JOIN students s ON s.user_id = ua.user_id " +
                    "WHERE c.course_id = ? AND e.status = 'enrolled' " +
                    "ORDER BY sec.section_id, ua.username";

                try (PreparedStatement ps2 = conn.prepareStatement(sqlCourse)) {
                    ps2.setInt(1, courseOrSectionId);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        while (rs2.next()) {
                            Map<String,Object> r = new HashMap<>();
                            r.put("user_id", rs2.getInt("user_id"));
                            r.put("username", rs2.getString("username"));
                            r.put("roll_no", rs2.getString("roll_no"));
                            r.put("program", rs2.getString("program"));
                            r.put("enrollment_id", rs2.getInt("enrollment_id"));
                            r.put("section_id", rs2.getInt("section_id"));
                            r.put("day_time", rs2.getString("day_time"));
                            out.add(r);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return out;
    }

    public static List<Map<String,Object>> listGradesForStudent(int studentUserId) {
        List<Map<String,Object>> out = new ArrayList<>();

        String sql =
            "SELECT " +
            "   c.code AS course_code, " +
            "   c.title AS course_title, " +
            "   sec.section_id AS section_label, " +
            "   e.enrollment_id AS enrollment_id, " +
            "   e.status AS enrollment_status, " +
            "   ec.name AS component, " +
            "   g.score AS score, " +
            "   COALESCE(ec.max_score, 100) AS max_score, " +
            "   ec.weight AS weight_pct, " +
            "   g.final_grade AS final_grade, " +
            "   c.credits AS credits, " +
            "   COALESCE(ua.username, 'Not Assigned') AS instructor_name " +
            "FROM enrollments e " +
            "JOIN sections sec ON e.section_id = sec.section_id " +
            "JOIN courses c ON sec.course_id = c.course_id " +
            "JOIN evaluation_components ec ON ec.section_id = sec.section_id " +  // master list
            "LEFT JOIN grades g ON e.enrollment_id = g.enrollment_id " +           // match grades if they exist
            "     AND UPPER(TRIM(ec.name)) = UPPER(TRIM(g.component)) " +
            "LEFT JOIN univ_auth.users_auth ua ON ua.user_id = sec.instructor_id " +
            "WHERE e.student_id = ? " +
            "ORDER BY c.code, sec.section_id, ec.name";

        try (Connection conn = DBManager.getErpConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentUserId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> row = new HashMap<>();
                    row.put("course_code", rs.getString("course_code"));
                    row.put("course_title", rs.getString("course_title"));
                    row.put("section_label", rs.getObject("section_label"));
                    row.put("enrollment_id", rs.getObject("enrollment_id"));
                    row.put("enrollment_status", rs.getString("enrollment_status")); // NEW
                    row.put("component", rs.getString("component"));
                    
                    // Handle nullable score
                    Object scoreObj = rs.getObject("score");
                    row.put("score", scoreObj); // Can be null
                    
                    row.put("max_score", rs.getObject("max_score"));
                    row.put("weight_pct", rs.getObject("weight_pct"));
                    row.put("final_grade", rs.getString("final_grade"));
                    row.put("credits", rs.getObject("credits"));
                    row.put("instructor_name", rs.getString("instructor_name"));
                    out.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return out;
    }

    public static Double calculateGPA(int studentUserId) {
        Map<String,Object> gpaMap = computeGpaForStudent(studentUserId);
        return (Double) gpaMap.get("overallGpaWeightedByCredits");
    }

    public static Map<String,Object> computeGpaForStudent(int studentUserId) {
        Map<String,Object> out = new HashMap<>();
        
        System.out.println("[StudentService] Computing GPA for student ID: " + studentUserId);
        String sql = "SELECT g.score, c.credits, c.code " +
                     "FROM grades g " +
                     "JOIN enrollments e ON g.enrollment_id = e.enrollment_id " +
                     "JOIN sections sec ON e.section_id = sec.section_id " +
                     "JOIN courses c ON sec.course_id = c.course_id " +
                     "WHERE e.student_id = ? " +
                     "AND g.component = 'FINAL_TOTAL' " +
                     "AND g.score IS NOT NULL " +
                     "AND e.status = 'enrolled'";

        double sumPoints = 0.0;
        int count = 0;
        double totalQualityPoints = 0.0;
        double totalCredits = 0.0;

        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object scoreObj = rs.getObject("score");
                    Number creditsObj = (Number) rs.getObject("credits");
                    
                    double credits = (creditsObj == null || creditsObj.doubleValue() <= 0.0) ? 3.0 : creditsObj.doubleValue();
                    double gradePoint = 0.0;

                    if (scoreObj != null) {
                        try {
                            double perc = Double.parseDouble(scoreObj.toString());
                            gradePoint = percentToPoint(perc);
                        } catch (Exception ignored) {}
                    }

                    sumPoints += gradePoint;
                    count++;
                    totalQualityPoints += gradePoint * credits;
                    totalCredits += credits;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        Double overall = (count == 0) ? null : Math.round((sumPoints / count) * 100.0) / 100.0;
        Double weighted = (totalCredits == 0.0) ? null : Math.round((totalQualityPoints / totalCredits) * 100.0) / 100.0;

        out.put("overallGpa", overall);
        out.put("overallGpaWeightedByCredits", weighted);
        return out;
    }

    private static double percentToPoint(double p) {
        if (p >= 93) return 4.0;
        if (p >= 90) return 3.7;
        if (p >= 87) return 3.3;
        if (p >= 83) return 3.0;
        if (p >= 80) return 2.7;
        if (p >= 77) return 2.3;
        if (p >= 73) return 2.0;
        if (p >= 70) return 1.7;
        if (p >= 67) return 1.3;
        if (p >= 63) return 1.0;
        return 0.0;
    }

    public static String dropStudentFromSection(int studentUserId, int sectionId) {
        if (maintenanceChecker.isMaintenanceOn()) {
            return "MAINTENANCE";
        }

        Connection conn = null;
        try {
            conn = DBManager.getErpConnection();
            conn.setAutoCommit(false);

            final String selectSectionSql = "SELECT drop_deadline FROM sections WHERE section_id = ?";
            final String selectEnrollSql  = "SELECT enrollment_id, status FROM enrollments WHERE student_id = ? AND section_id = ? LIMIT 1";
            final String updateEnrollSql  = "UPDATE enrollments SET status = 'dropped', dropped_at = NOW() WHERE enrollment_id = ?";
            final String deleteGradesSql  = "DELETE FROM grades WHERE enrollment_id = ?";

            // 1) fetch section drop_deadline
            java.sql.Timestamp dropDeadlineTs = null;
            try (PreparedStatement ps = conn.prepareStatement(selectSectionSql)) {
                ps.setInt(1, sectionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return "NOT_FOUND";
                    }
                    dropDeadlineTs = rs.getTimestamp("drop_deadline"); // may be null
                }
            }

            // 2) if deadline exists, compare with now (use server time)
            if (dropDeadlineTs != null) {
                java.time.Instant now = java.time.Instant.now();
                java.time.Instant deadlineInstant = dropDeadlineTs.toInstant();
                if (now.isAfter(deadlineInstant)) {
                    conn.rollback();
                    return "AFTER_DEADLINE";
                }
            }

            // 3) ensure student is enrolled (fetch enrollment_id)
            Integer enrollmentId = null;
            String currentStatus = null;
            try (PreparedStatement ps = conn.prepareStatement(selectEnrollSql)) {
                ps.setInt(1, studentUserId);
                ps.setInt(2, sectionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        enrollmentId = rs.getInt("enrollment_id");
                        currentStatus = rs.getString("status");
                    } else {
                        conn.rollback();
                        return "NOT_ENROLLED";
                    }
                }
            }

            // If already dropped, simply return NOT_ENROLLED or a special code if you prefer.
            if ("dropped".equalsIgnoreCase(currentStatus)) {
                conn.rollback();
                return "NOT_ENROLLED";
            }

            // 4) delete grades for this enrollment (so scores vanish)
            try (PreparedStatement ps = conn.prepareStatement(deleteGradesSql)) {
                ps.setInt(1, enrollmentId);
                ps.executeUpdate();
            }

            // 5) perform soft-drop (update status and dropped_at)
            try (PreparedStatement ps = conn.prepareStatement(updateEnrollSql)) {
                ps.setInt(1, enrollmentId);
                int updated = ps.executeUpdate();
                if (updated != 1) {
                    conn.rollback();
                    return "ERROR";
                }
            }

            conn.commit();
            return "OK";

        } catch (SQLException e) {
            // ensure rollback on failure
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ignored) {}

            e.printStackTrace();
            return "ERROR";

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (Exception ignore) {}
            }
        }
    }


    public static String dropEnrollmentSafe(int studentUserId, int sectionId) {
        return dropStudentFromSection(studentUserId, sectionId);
    }
    
}