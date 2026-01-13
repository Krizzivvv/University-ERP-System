package edu.univ.erp.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.univ.erp.db.DBManager;
import edu.univ.erp.util.MaintenanceChecker;

public class GradeService {

    private static final double W_QUIZ = 0.20;
    private static final double W_MID  = 0.30;
    private static final double W_FINAL = 0.50;
    private static final MaintenanceChecker maintenanceChecker = new MaintenanceChecker();

    private static void checkMaintenanceMode() throws SQLException {
        if (maintenanceChecker.isMaintenanceOn()) {
            throw new SQLException("Maintenance mode is ON. Cannot perform write operations.");
        }
    }

    private static void validateAccessAndMaintenance(int userIdPerformingAction, int enrollmentId) throws SQLException {
        checkMaintenanceMode();
        int sectionId = SectionService.getSectionIdForEnrollment(enrollmentId);
        if (!SectionService.isInstructorAssignedToSection(userIdPerformingAction, sectionId)) {
            throw new SQLException("Access Denied: Not your section.");
        }
    }

    private static void validateSectionAccessAndMaintenance(int userIdPerformingAction, int sectionId) throws SQLException {
        checkMaintenanceMode();
        if (!SectionService.isInstructorAssignedToSection(userIdPerformingAction, sectionId)) {
            throw new SQLException("Access Denied: Not your section.");
        }
    }

    public static void setComponentScore(int userIdPerformingAction, int enrollmentId, String component, double score) throws SQLException {
        validateAccessAndMaintenance(userIdPerformingAction, enrollmentId);

        String sql = "INSERT INTO grades (enrollment_id, component, score) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE score = VALUES(score)";
        try (Connection c = DBManager.getErpConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            ps.setString(2, component);
            ps.setDouble(3, score);
            ps.executeUpdate();
        }
    }
    
    public static void saveComponentsAndCompute(int userIdPerformingAction, int enrollmentId, Map<String, Number> compMap) throws SQLException {
        validateAccessAndMaintenance(userIdPerformingAction, enrollmentId);

        Connection c = null;
        try {
            c = DBManager.getErpConnection();
            c.setAutoCommit(false);
            
            // Get canonical component names
            Map<String, String> canonical = new HashMap<>();
            String fetchCanonical = "SELECT ec.name FROM enrollments e JOIN sections s ON e.section_id = s.section_id JOIN evaluation_components ec ON ec.section_id = s.section_id WHERE e.enrollment_id = ?";
            try (PreparedStatement ps = c.prepareStatement(fetchCanonical)) {
                ps.setInt(1, enrollmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String comp = rs.getString("name");
                        if (comp != null) canonical.put(comp.toLowerCase().trim(), comp);
                    }
                }
            }

            System.out.println("[GradeService] saveComponentsAndCompute START enrollment=" + enrollmentId + " incomingCompMap=" + compMap + " canonical=" + canonical);

            // Upsert component scores
            String upsert = "INSERT INTO grades (enrollment_id, component, score) VALUES (?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE score = VALUES(score)";
            try (PreparedStatement ps = c.prepareStatement(upsert)) {
                for (Map.Entry<String, Number> ent : compMap.entrySet()) {
                    String rawName = ent.getKey();
                    Number score = ent.getValue();

                    String mapped = canonical.getOrDefault(rawName.toLowerCase().trim(), rawName);
                    ps.setInt(1, enrollmentId);
                    ps.setString(2, mapped);
                    if (score == null) {
                        ps.setObject(3, null);
                    } else {
                        ps.setDouble(3, score.doubleValue());
                    }
                    int affected = ps.executeUpdate();
                    System.out.println("[GradeService] upsert comp=" + mapped + " score=" + score + " affected=" + affected);
                }
            }

            Map<String, Double> weights = new HashMap<>();
            String fetchWeights = "SELECT ec.name, ec.weight FROM enrollments e JOIN sections s ON e.section_id = s.section_id JOIN evaluation_components ec ON ec.section_id = s.section_id WHERE e.enrollment_id = ?";
            try (PreparedStatement ps = c.prepareStatement(fetchWeights)) {
                ps.setInt(1, enrollmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        weights.put(rs.getString("name"), rs.getDouble("weight"));
                    }
                }
            }

            System.out.println("[GradeService] Weights loaded: " + weights);

            // Compute final score
            double total = 0.0;
            double totalWeight = 0.0;
            for (Map.Entry<String, Number> ent : compMap.entrySet()) {
                String rawName = ent.getKey();
                Number scoreNum = ent.getValue();
                if (scoreNum == null) continue;

                String mapped = canonical.getOrDefault(rawName.toLowerCase().trim(), rawName);
                Double w = weights.get(mapped);
                if (w == null) {
                    System.out.println("[GradeService] WARNING: No weight found for component: " + mapped);
                    continue;
                }

                double scoreVal = scoreNum.doubleValue();
                total += (scoreVal / 100.0) * w;
                totalWeight += w;
            }

            double finalScore = (totalWeight == 0.0) ? 0.0 : (total / totalWeight) * 100.0;
            finalScore = Math.round(finalScore * 100.0) / 100.0;

            System.out.println("[GradeService] Computed finalScore=" + finalScore + " from total=" + total + " totalWeight=" + totalWeight);

            // Save FINAL_TOTAL
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO grades (enrollment_id, component, score) VALUES (?, 'FINAL_TOTAL', ?) " +
                            "ON DUPLICATE KEY UPDATE score = VALUES(score)")) {
                ps.setInt(1, enrollmentId);
                ps.setDouble(2, finalScore);
                ps.executeUpdate();
                System.out.println("[GradeService] wrote FINAL_TOTAL=" + finalScore + " for enrollment=" + enrollmentId);
            }

            // Save FINAL_LETTER
            String letter = numericToLetter(finalScore);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO grades (enrollment_id, component, final_grade) VALUES (?, 'FINAL_LETTER', ?) " +
                            "ON DUPLICATE KEY UPDATE final_grade = VALUES(final_grade)")) {
                ps.setInt(1, enrollmentId);
                ps.setString(2, letter);
                ps.executeUpdate();
                System.out.println("[GradeService] wrote FINAL_LETTER=" + letter + " for enrollment=" + enrollmentId);
            }

            c.commit();
            System.out.println("[GradeService] saveComponentsAndCompute END enrollment=" + enrollmentId);
        } catch (SQLException ex) {
            if (c != null) {
                try { c.rollback(); } catch (Exception ignored) {}
            }
            throw ex;
        } finally {
            if (c != null) {
                try { c.setAutoCommit(true); c.close(); } catch (Exception ignored) {}
            }
        }
    }

    private static void computeAndSaveFinalDynamic(Connection conn, int enrollmentId) throws SQLException {
        if (conn == null) throw new SQLException("Connection is null");

        System.out.println("[GradeService] ====== COMPUTING FINAL GRADE ======");
        System.out.println("[GradeService] Enrollment ID: " + enrollmentId);

        Integer sectionId = null;
        String qSection = "SELECT section_id FROM enrollments WHERE enrollment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(qSection)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) sectionId = rs.getInt("section_id");
            }
        }
        
        if (sectionId == null) {
            throw new SQLException("Could not find section for enrollment " + enrollmentId);
        }
        
        System.out.println("[GradeService] Section ID: " + sectionId);

        Map<String, BigDecimal> scoreMap = new HashMap<>();
        String qScores = "SELECT `component`, `score` FROM `grades` WHERE `enrollment_id` = ? AND component IN ('Midsem', 'Endsem', 'Assignment', 'Quiz')";
        try (PreparedStatement ps = conn.prepareStatement(qScores)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String comp = rs.getString("component");
                    BigDecimal bd = rs.getBigDecimal("score");
                    if (bd == null) continue;
                    scoreMap.put(comp, bd);
                    System.out.println("[GradeService] Component: " + comp + ", Score: " + bd);
                }
            }
        }

        if (scoreMap.isEmpty()) {
            System.out.println("[GradeService] No component scores found, skipping final computation");
            return;
        }

        // Load weights from evaluation_components
        Map<String, BigDecimal> weightMap = new HashMap<>();
        String qWeights = "SELECT `name`, `weight` FROM `evaluation_components` WHERE `section_id` = ?";
        try (PreparedStatement ps = conn.prepareStatement(qWeights)) {
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String comp = rs.getString("name");
                    BigDecimal w = rs.getBigDecimal("weight");
                    if (w != null) {
                        weightMap.put(comp, w);
                        System.out.println("[GradeService] Weight: " + comp + " = " + w + "%");
                    }
                }
            }
        }

        if (weightMap.isEmpty()) {
            System.out.println("[GradeService] WARNING: No weights found, using equal weights");
        }
        
        Map<String, BigDecimal> maxScoreMap = new HashMap<>();
        String qMaxScores = "SELECT `name`, `max_score` FROM `evaluation_components` WHERE `section_id` = ?";
        try (PreparedStatement ps = conn.prepareStatement(qMaxScores)) {
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String comp = rs.getString("name");
                    BigDecimal maxScore = rs.getBigDecimal("max_score");
                    if (maxScore != null) {
                        maxScoreMap.put(comp, maxScore);
                    }
                }
            }
        }

        // Calculate weighted final score
        BigDecimal totalPoints = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : weightMap.entrySet()) {
            String comp = entry.getKey();
            BigDecimal weight = entry.getValue();
            
            BigDecimal score = null;
            for (Map.Entry<String, BigDecimal> scoreEntry : scoreMap.entrySet()) {
                if (scoreEntry.getKey().equalsIgnoreCase(comp)) {
                    score = scoreEntry.getValue();
                    break;
                }
            }
            
            BigDecimal maxScore = null;
            for (Map.Entry<String, BigDecimal> maxEntry : maxScoreMap.entrySet()) {
                if (maxEntry.getKey().equalsIgnoreCase(comp)) {
                    maxScore = maxEntry.getValue();
                    break;
                }
            }
            
            if (score != null && weight != null && maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = score.divide(maxScore, 4, java.math.RoundingMode.HALF_UP);
                BigDecimal contribution = percentage.multiply(weight);
                totalPoints = totalPoints.add(contribution);
                totalWeight = totalWeight.add(weight);
                
                System.out.println("[GradeService] " + comp + ": score=" + score + "/" + maxScore + 
                                " (" + percentage.multiply(new BigDecimal("100")).setScale(2, java.math.RoundingMode.HALF_UP) + "%), " +
                                "weight=" + weight + "%, contribution=" + contribution.setScale(2, java.math.RoundingMode.HALF_UP));
            }
        }

        double numericFinal = totalPoints.doubleValue();
        System.out.println("[GradeService] TOTAL WEIGHTED SCORE: " + numericFinal);
        System.out.println("[GradeService] TOTAL WEIGHT: " + totalWeight);

        String upsertTotal = "INSERT INTO grades (enrollment_id, component, score) VALUES (?, 'FINAL_TOTAL', ?) ON DUPLICATE KEY UPDATE score = VALUES(score)";
        try (PreparedStatement ps = conn.prepareStatement(upsertTotal)) {
            ps.setInt(1, enrollmentId);
            ps.setDouble(2, numericFinal);
            int rows = ps.executeUpdate();
            System.out.println("[GradeService] ✓ Saved FINAL_TOTAL=" + numericFinal + " (rows affected: " + rows + ")");
        }

        String letter = percentageToLetterGrade(numericFinal);
        String upsertLetter = "INSERT INTO grades (enrollment_id, component, final_grade) VALUES (?, 'FINAL_LETTER', ?) ON DUPLICATE KEY UPDATE final_grade = VALUES(final_grade)";
        try (PreparedStatement ps = conn.prepareStatement(upsertLetter)) {
            ps.setInt(1, enrollmentId);
            ps.setString(2, letter);
            int rows = ps.executeUpdate();
            System.out.println("[GradeService] ✓ Saved FINAL_LETTER=" + letter + " (rows affected: " + rows + ")");
        }

        System.out.println("[GradeService] ====== FINAL GRADE COMPUTATION COMPLETE ======");
    }
    public static void computeAndSaveFinal(int userIdPerformingAction, int enrollmentId) throws SQLException {
        validateAccessAndMaintenance(userIdPerformingAction, enrollmentId);
        
        Connection conn = null;
        try {
            conn = DBManager.getErpConnection();
            conn.setAutoCommit(false);
            
            computeAndSaveFinalDynamic(conn, enrollmentId);
            
            conn.commit();
        } catch (SQLException ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            throw ex;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    public static String numericToLetter(double num) {
        return percentageToLetterGrade(num);
    }

    public static String percentageToLetterGrade(double percentage) {
        if (percentage >= 95.0) return "A+";
        if (percentage >= 90.0) return "A";
        if (percentage >= 85.0) return "A-";
        if (percentage >= 80.0) return "B+";
        if (percentage >= 75.0) return "B";
        if (percentage >= 70.0) return "B-";
        if (percentage >= 65.0) return "C+";
        if (percentage >= 60.0) return "C";
        if (percentage >= 55.0) return "C-";
        if (percentage >= 50.0) return "D";
        return "F";
    }

    public static void setAllAndCompute(int userIdPerformingAction, int enrollmentId, double quiz, double mid, double fin) throws SQLException {
        validateAccessAndMaintenance(userIdPerformingAction, enrollmentId);

        Connection c = null;
        try {
            c = DBManager.getErpConnection();
            c.setAutoCommit(false);

            String ins = "INSERT INTO grades (enrollment_id, component, score) VALUES (?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE score = VALUES(score)";

            try (PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setInt(1, enrollmentId);
                ps.setString(2, "QUIZ");
                ps.setDouble(3, quiz);
                ps.executeUpdate();

                ps.setInt(1, enrollmentId);
                ps.setString(2, "MID");
                ps.setDouble(3, mid);
                ps.executeUpdate();

                ps.setInt(1, enrollmentId);
                ps.setString(2, "FINAL");
                ps.setDouble(3, fin);
                ps.executeUpdate();
            }

            double numericFinal = quiz * W_QUIZ + mid * W_MID + fin * W_FINAL;
            numericFinal = Math.round(numericFinal * 100.0) / 100.0;
            String upsertTotal = "INSERT INTO grades (enrollment_id, component, score) VALUES (?, 'FINAL_TOTAL', ?) " +
                                 "ON DUPLICATE KEY UPDATE score = VALUES(score)";
            try (PreparedStatement ps2 = c.prepareStatement(upsertTotal)) {
                ps2.setInt(1, enrollmentId);
                ps2.setDouble(2, numericFinal);
                ps2.executeUpdate();
            }

            String letter = numericToLetter(numericFinal);
            String upsertLetter = "INSERT INTO grades (enrollment_id, component, final_grade) VALUES (?, 'FINAL_LETTER', ?) " +
                                  "ON DUPLICATE KEY UPDATE final_grade = VALUES(final_grade)";
            try (PreparedStatement ps3 = c.prepareStatement(upsertLetter)) {
                ps3.setInt(1, enrollmentId);
                ps3.setString(2, letter);
                ps3.executeUpdate();
            }

            c.commit();
        } catch (SQLException ex) {
            if (c != null) try { c.rollback(); } catch (SQLException ignored) {}
            throw ex;
        } finally {
            if (c != null) try { c.setAutoCommit(true); c.close(); } catch (SQLException ignored) {}
        }
    }

    public static Double getComponentScore(int enrollmentId, String component) throws SQLException {
        String sql = "SELECT score FROM grades WHERE enrollment_id = ? AND component = ?";
        try (Connection c = DBManager.getErpConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            ps.setString(2, component);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double v = rs.getDouble("score");
                    if (rs.wasNull()) return null;
                    return v;
                } else {
                    return null;
                }
            }
        }
    }

    public static String getFinalLetter(int enrollmentId) throws SQLException {
        String sql = "SELECT final_grade FROM grades WHERE enrollment_id = ? AND component = 'FINAL_LETTER' LIMIT 1";
        try (Connection c = DBManager.getErpConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("final_grade");
                } else {
                    return null;
                }
            }
        }
    }

    public static Integer getCourseIdForSection(int sectionId) {
        String sql = "SELECT course_id FROM sections WHERE section_id = ?";
        
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, sectionId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("course_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }

    public static List<Map<String, Object>> listGradesForEnrollment(int enrollmentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        String sql = "SELECT grade_id, component, score, final_grade " +
                     "FROM grades " +
                     "WHERE enrollment_id = ? " +
                     "ORDER BY grade_id";
        
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, enrollmentId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("grade_id", rs.getInt("grade_id"));
                    row.put("component", rs.getString("component"));
                    row.put("score", rs.getBigDecimal("score"));
                    row.put("final_grade", rs.getString("final_grade"));
                    result.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return result;
    }

    public static boolean insertGrade(int userIdPerformingAction, int enrollmentId, String component, BigDecimal score, String finalGrade) {
        try {
            validateAccessAndMaintenance(userIdPerformingAction, enrollmentId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        String sql = "INSERT INTO grades (enrollment_id, component, score, final_grade) " +
                     "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, enrollmentId);
            ps.setString(2, component);
            
            if (score == null) {
                ps.setNull(3, Types.DECIMAL);
            } else {
                ps.setBigDecimal(3, score);
            }
            
            if (finalGrade == null || finalGrade.trim().isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, finalGrade);
            }
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateGrade(int userIdPerformingAction, int gradeId, String component, BigDecimal score, String finalGrade) {
        // First, get the enrollment_id for this grade to validate access
        int enrollmentId;
        try {
            enrollmentId = getEnrollmentIdForGrade(gradeId);
            validateAccessAndMaintenance(userIdPerformingAction, enrollmentId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        String sql = "UPDATE grades " +
                     "SET component = ?, score = ?, final_grade = ? " +
                     "WHERE grade_id = ?";
        
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, component);
            
            if (score == null) {
                ps.setNull(2, Types.DECIMAL);
            } else {
                ps.setBigDecimal(2, score);
            }
            
            if (finalGrade == null || finalGrade.trim().isEmpty()) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, finalGrade);
            }
            
            ps.setInt(4, gradeId);
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteGrade(int userIdPerformingAction, int gradeId) {
        // First, get the enrollment_id for this grade to validate access
        int enrollmentId;
        try {
            enrollmentId = getEnrollmentIdForGrade(gradeId);
            validateAccessAndMaintenance(userIdPerformingAction, enrollmentId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        String sql = "DELETE FROM grades WHERE grade_id = ?";
        
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, gradeId);
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static int getEnrollmentIdForGrade(int gradeId) throws SQLException {
        String sql = "SELECT enrollment_id FROM grades WHERE grade_id = ?";
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gradeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("enrollment_id");
                } else {
                    throw new SQLException("Grade not found with grade_id: " + gradeId);
                }
            }
        }
    }

    public static List<Map<String, Object>> listEvaluationComponentsForCourse(int courseId) {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT DISTINCT ec.component_id, ec.name, ec.max_score, ec.weight " +
                     "FROM evaluation_components ec " +
                     "JOIN sections s ON ec.section_id = s.section_id " +
                     "WHERE s.course_id = ? " +
                     "ORDER BY ec.component_id";
        
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, courseId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("component_id", rs.getInt("component_id"));
                    row.put("component_name", rs.getString("name"));
                    row.put("max_score", rs.getBigDecimal("max_score"));
                    row.put("weight_pct", rs.getBigDecimal("weight"));
                    result.add(row);
                }
            }
            
            System.out.println("[DEBUG] GradeService.listEvaluationComponentsForCourse(courseId=" + courseId + ") -> " + result.size() + " components");
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("[ERROR] Failed to list evaluation components for course " + courseId + ": " + e.getMessage());
        }
        
        return result;
    }

    public static boolean upsertGrade(int userIdPerformingAction, int enrollmentId, String component, BigDecimal score, String finalGrade) {
        try {
            validateAccessAndMaintenance(userIdPerformingAction, enrollmentId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (score != null) {
            String checkMaxSql = "SELECT ec.max_score, ec.name " +
                                "FROM evaluation_components ec " +
                                "JOIN sections s ON ec.section_id = s.section_id " +
                                "JOIN enrollments e ON e.section_id = s.section_id " +
                                "WHERE e.enrollment_id = ? AND UPPER(TRIM(ec.name)) = UPPER(TRIM(?))";
            
            try (Connection conn = DBManager.getErpConnection();
                PreparedStatement ps = conn.prepareStatement(checkMaxSql)) {
                
                ps.setInt(1, enrollmentId);
                ps.setString(2, component);
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigDecimal maxScore = rs.getBigDecimal("max_score");
                        if (maxScore != null && score.compareTo(maxScore) > 0) {
                            String compName = rs.getString("name");
                            throw new RuntimeException("Score " + score + " exceeds maximum allowed score of " + maxScore + " for component '" + compName + "'");
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("[ERROR] Failed to validate score: " + e.getMessage());
                throw new RuntimeException("Failed to validate score: " + e.getMessage());
            }
        }

        String sql = "INSERT INTO grades (enrollment_id, component, score, final_grade) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE score = VALUES(score), final_grade = VALUES(final_grade)";
        
        try (Connection conn = DBManager.getErpConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, enrollmentId);
            ps.setString(2, component);
            
            if (score == null) {
                ps.setNull(3, Types.DECIMAL);
            } else {
                ps.setBigDecimal(3, score);
            }
            
            if (finalGrade == null || finalGrade.trim().isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, finalGrade);
            }
            
            int rows = ps.executeUpdate();
            System.out.println("[DEBUG] GradeService.upsertGrade(enrollmentId=" + enrollmentId + ", component=" + component + ") -> affected " + rows + " rows");
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("[ERROR] Failed to upsert grade: " + e.getMessage());
            return false;
        }
    }
    public static boolean updateComponentMaxScore(int userIdPerformingAction, int sectionId, String componentName, BigDecimal maxScore) {
        try {
            validateSectionAccessAndMaintenance(userIdPerformingAction, sectionId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        String sql = "UPDATE evaluation_components " +
                     "SET max_score = ? " +
                     "WHERE section_id = ? AND UPPER(name) = UPPER(?)";
        
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setBigDecimal(1, maxScore);
            ps.setInt(2, sectionId);
            ps.setString(3, componentName);
            
            int rows = ps.executeUpdate();
            System.out.println("[DEBUG] GradeService.updateComponentMaxScore(sectionId=" + sectionId + 
                             ", component=" + componentName + ", maxScore=" + maxScore + ") -> updated " + rows + " rows");
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("[ERROR] Failed to update component max score: " + e.getMessage());
            return false;
        }
    }

     public static Map<String, Object> getClassStats(int sectionId) {

        String sql = "SELECT " +
                     "AVG(CASE WHEN g.component = 'QUIZ' THEN g.score END) as avg_quiz, " +
                     "AVG(CASE WHEN g.component = 'MID' THEN g.score END) as avg_midterm, " +
                     "AVG(CASE WHEN g.component = 'FINAL' THEN g.score END) as avg_final_exam, " +
                     "AVG(CASE WHEN g.component = 'FINAL_TOTAL' THEN g.score END) as avg_final_grade, " +
                     "COUNT(DISTINCT e.student_id) as class_size " +
                     "FROM enrollments e " +
                     "LEFT JOIN grades g ON e.enrollment_id = g.enrollment_id " +
                     "WHERE e.section_id = ?";

        Map<String, Object> stats = new HashMap<>();
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sectionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("avg_quiz", rs.getObject("avg_quiz"));
                    stats.put("avg_midterm", rs.getObject("avg_midterm"));
                    stats.put("avg_final_exam", rs.getObject("avg_final_exam"));
                    stats.put("avg_final_grade", rs.getObject("avg_final_grade"));
                    stats.put("class_size", rs.getObject("class_size"));
                }
            }
        } catch (SQLException e) {
            Logger.getLogger(GradeService.class.getName()).log(Level.SEVERE, "Error fetching stats for section " + sectionId, e);
            return null;
        }
        return stats;
    }

} 