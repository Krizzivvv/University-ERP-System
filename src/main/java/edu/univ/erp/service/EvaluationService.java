package edu.univ.erp.service;

import edu.univ.erp.db.DBManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvaluationService {
    public static List<Map<String,Object>> listComponentsForSection(int sectionId) {
        List<Map<String,Object>> out = new ArrayList<>();
        String sql = "SELECT component_id, name, weight, max_score " +
                     "FROM evaluation_components " +
                     "WHERE section_id = ? " +
                     "ORDER BY component_id";

        try (Connection c = DBManager.getErpConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> m = new HashMap<>();
                    m.put("component_id", rs.getInt("component_id"));
                    m.put("name", rs.getString("name"));
                    m.put("weight", rs.getDouble("weight"));
                    m.put("max_score", rs.getDouble("max_score"));
                    out.add(m);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return out;
    }

    public static boolean addComponent(int sectionId, String name, double weight, double maxScore) {
        String sql = "INSERT INTO evaluation_components (section_id, name, weight, max_score) VALUES (?, ?, ?, ?)";
        try (Connection c = DBManager.getErpConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            ps.setString(2, name);
            ps.setDouble(3, weight);
            ps.setDouble(4, maxScore);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean updateComponent(int componentId, String name, double weight, double maxScore) {
        String sql = "UPDATE evaluation_components SET name = ?, weight = ?, max_score = ? WHERE component_id = ?";
        try (Connection c = DBManager.getErpConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, weight);
            ps.setDouble(3, maxScore);
            ps.setInt(4, componentId);
            int r = ps.executeUpdate();
            return r > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean deleteComponent(int componentId) {
        String sql = "DELETE FROM evaluation_components WHERE component_id = ?";
        try (Connection c = DBManager.getErpConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, componentId);
            int r = ps.executeUpdate();
            return r > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}