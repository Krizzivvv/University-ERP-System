package edu.univ.erp.service;

import edu.univ.erp.db.DBManager;
import edu.univ.erp.model.CourseSection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CatalogService {

    public static List<CourseSection> listAllSections() {
        List<CourseSection> out = new ArrayList<>();
        String sql = "SELECT s.section_id, s.course_id, c.code, c.title, s.day_time, s.room, s.capacity, s.semester, s.year, s.instructor_id, " +
                     "(SELECT COUNT(*) FROM enrollments e WHERE e.section_id = s.section_id AND e.status='enrolled') AS enrolled " +
                     "FROM sections s JOIN courses c ON s.course_id = c.course_id " +
                     "ORDER BY c.code, s.section_id";
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                CourseSection cs = new CourseSection();
                cs.setSectionId(rs.getInt("section_id"));
                cs.setCourseId(rs.getInt("course_id"));
                cs.setCourseCode(rs.getString("code"));
                cs.setCourseTitle(rs.getString("title"));
                cs.setDayTime(rs.getString("day_time"));
                cs.setRoom(rs.getString("room"));
                cs.setCapacity(rs.getInt("capacity"));
                cs.setSemester(rs.getString("semester"));
                cs.setYear(rs.getInt("year"));
                cs.setInstructorId((Integer) (rs.getObject("instructor_id")));
                cs.setEnrolled(rs.getInt("enrolled"));
                out.add(cs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }
}
