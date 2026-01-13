package edu.univ.erp.ui;

import edu.univ.erp.db.DBManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class TimetablePanel extends JPanel {
    private final int studentUserId;
    private JTable timetableGrid;
    private DefaultTableModel gridModel;
    private JTable legendTable;
    private DefaultTableModel legendModel;

    private static final Color BACKGROUND = new Color(245, 239, 221);      // Light sepia background
    private static final Color PANEL = new Color(231, 220, 197);           // Deeper sepia for panels
    private static final Color PRIMARY = new Color(46, 79, 79);            // Wine green for primary actions
    private static final Color ACCENT = new Color(158, 179, 132);          // Sage green for accents
    private static final Color TEXT_PRIMARY = new Color(62, 39, 35);       // Coffee brown for main text

    private static final String[] TIME_SLOTS = {
        "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00",
        "12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00",
        "16:00-17:00", "17:00-18:00"
    };
    
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    public TimetablePanel(int studentUserId) {
        this.studentUserId = studentUserId;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(BACKGROUND); 
        initUI();
        loadTimetable();
    }

    private void initUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BACKGROUND);
        
        JLabel title = new JLabel("My Weekly Timetable");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(PRIMARY); 
        
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.setBackground(PRIMARY);
        btnRefresh.setForeground(BACKGROUND);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnRefresh.addActionListener(e -> loadTimetable());
        
        header.add(title, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel gridViewPanel = createGridViewPanel();
        add(gridViewPanel, BorderLayout.CENTER);
    }

    private JPanel createGridViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BACKGROUND);
        
        // Grid Table
        String[] gridCols = new String[DAYS.length + 1];
        gridCols[0] = "Time";
        System.arraycopy(DAYS, 0, gridCols, 1, DAYS.length);
        
        gridModel = new DefaultTableModel(gridCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        timetableGrid = new JTable(gridModel);
        timetableGrid.setRowHeight(50);
        timetableGrid.setFont(new Font("SansSerif", Font.PLAIN, 12));
        timetableGrid.setForeground(TEXT_PRIMARY);
        timetableGrid.setBackground(BACKGROUND);
        timetableGrid.setGridColor(ACCENT);
        timetableGrid.setSelectionBackground(ACCENT);
        timetableGrid.setSelectionForeground(BACKGROUND);
        
        // Header
        JTableHeader header = timetableGrid.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(BACKGROUND);
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setBorder(new LineBorder(ACCENT));

        timetableGrid.getColumnModel().getColumn(0).setPreferredWidth(80);
        for (int i = 1; i < gridCols.length; i++) {
            timetableGrid.getColumnModel().getColumn(i).setPreferredWidth(120);
        }
        
        timetableGrid.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column > 0 && value != null && !value.toString().isEmpty()) {
                    c.setBackground(ACCENT); 
                    c.setForeground(BACKGROUND); 
                    c.setFont(new Font("SansSerif", Font.BOLD, 11));
                } else {
                    c.setBackground(PANEL); 
                    c.setForeground(TEXT_PRIMARY);
                }
                
                if (isSelected) {
                    c.setBackground(PRIMARY);
                    c.setForeground(BACKGROUND);
                }
                
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
        
        JScrollPane gridScroll = new JScrollPane(timetableGrid);
        gridScroll.setPreferredSize(new Dimension(800, 350));
        gridScroll.getViewport().setBackground(BACKGROUND);
        gridScroll.setBorder(new LineBorder(ACCENT));
        panel.add(gridScroll, BorderLayout.CENTER);
        
        JPanel legendPanel = new JPanel(new BorderLayout(5, 5));
        legendPanel.setBackground(BACKGROUND);
        
        TitledBorder border = BorderFactory.createTitledBorder(new LineBorder(ACCENT), "Course Details");
        border.setTitleColor(PRIMARY);
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 12));
        legendPanel.setBorder(border);
        
        String[] legendCols = {"Course Code", "Course Title", "Section", "Day/Time", "Room", "Instructor"};
        legendModel = new DefaultTableModel(legendCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        legendTable = new JTable(legendModel);
        legendTable.setRowHeight(25);
        legendTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        legendTable.setForeground(TEXT_PRIMARY);
        legendTable.setBackground(BACKGROUND);
        legendTable.setGridColor(ACCENT);
        legendTable.setSelectionBackground(ACCENT);
        legendTable.setSelectionForeground(BACKGROUND);
        
        JTableHeader legendHeader = legendTable.getTableHeader();
        legendHeader.setBackground(PRIMARY);
        legendHeader.setForeground(BACKGROUND);
        legendHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        JScrollPane legendScroll = new JScrollPane(legendTable);
        legendScroll.setPreferredSize(new Dimension(800, 120));
        legendScroll.getViewport().setBackground(BACKGROUND);
        legendScroll.setBorder(new LineBorder(ACCENT));
        legendPanel.add(legendScroll, BorderLayout.CENTER);
        
        panel.add(legendPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private void loadTimetable() {
        gridModel.setRowCount(0);
        legendModel.setRowCount(0);

        for (String timeSlot : TIME_SLOTS) {
            Object[] row = new Object[DAYS.length + 1];
            row[0] = timeSlot;
            for (int i = 1; i <= DAYS.length; i++) {
                row[i] = "";
            }
            gridModel.addRow(row);
        }

        List<Map<String, Object>> sections = getEnrolledSections();
        
        for (Map<String, Object> sec : sections) {
            String courseCode = (String) sec.get("course_code");
            String courseTitle = (String) sec.get("course_title");
            String dayTime = (String) sec.get("day_time");
            String room = (String) sec.get("room");
            String instructor = sec.get("instructor") == null ? "Instructor Not Assigned" : (String) sec.get("instructor");
            int sectionId = (Integer) sec.get("section_id");

            legendModel.addRow(new Object[]{
                courseCode, courseTitle, "Sec " + sectionId, dayTime, room, instructor
            });

            if (dayTime != null && !dayTime.isEmpty()) {
                parseDayTimeAndAddToGrid(courseCode, dayTime, room);
            }
        }
    }

    private void parseDayTimeAndAddToGrid(String courseCode, String dayTime, String room) {
        if (dayTime == null || dayTime.trim().isEmpty()) return;
        
        String dt = dayTime.trim().toUpperCase();
        
        String[][] dayPatterns = {
            {"MONDAY", "1"}, {"MON", "1"},
            {"TUESDAY", "2"}, {"TUES", "2"}, {"TUE", "2"}, 
            {"WEDNESDAY", "3"}, {"WED", "3"},
            {"THURSDAY", "4"}, {"THURS", "4"}, {"THU", "4"}, {"TH", "4"},
            {"FRIDAY", "5"}, {"FRI", "5"},
            {"SATURDAY", "6"}, {"SAT", "6"}
        };

        int dayCol = -1;
        for (String[] pattern : dayPatterns) {
            if (dt.contains(pattern[0])) {
                dayCol = Integer.parseInt(pattern[1]);
                break;
            }
        }
        
        if (dayCol == -1) return;

        // Extract start and end hours from time range like "9:00-12:00"
        java.util.regex.Pattern timeRangePattern = java.util.regex.Pattern.compile("(\\d{1,2})(?::\\d{2})?\\s*-\\s*(\\d{1,2})(?::\\d{2})?");
        java.util.regex.Matcher matcher = timeRangePattern.matcher(dt);
        
        int startHour = -1;
        int endHour = -1;
        
        if (matcher.find()) {
            startHour = Integer.parseInt(matcher.group(1));
            endHour = Integer.parseInt(matcher.group(2));
        } else {
            // Fallback: single time
            java.util.regex.Pattern singleTimePattern = java.util.regex.Pattern.compile("(\\d{1,2})(?::\\d{2})?");
            java.util.regex.Matcher singleMatcher = singleTimePattern.matcher(dt);
            if (singleMatcher.find()) {
                startHour = Integer.parseInt(singleMatcher.group(1));
                endHour = startHour + 1;
            }
        }
        
        if (startHour == -1) return;
        
        // Build cell value: Course Code + Room
        String roomDisplay = (room == null || room.isEmpty() || "TBA".equalsIgnoreCase(room)) 
                             ? "" : " (" + room + ")";
        String cellValue = courseCode + roomDisplay;
        
        // Filling cells from startHour to endHour
        for (int hour = startHour; hour < endHour; hour++) {
            int rowIndex = hour - 8;
            if (rowIndex >= 0 && rowIndex < TIME_SLOTS.length) {
                gridModel.setValueAt(cellValue, rowIndex, dayCol);
            }
        }
    }

    private List<Map<String, Object>> getEnrolledSections() {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = """
            SELECT s.section_id, c.code AS course_code, c.title AS course_title,
                   s.day_time, s.room, ua.username AS instructor
            FROM enrollments e
            JOIN sections s ON e.section_id = s.section_id
            JOIN courses c ON s.course_id = c.course_id
            LEFT JOIN univ_auth.users_auth ua ON ua.user_id = s.instructor_id
            WHERE e.student_id = ? AND e.status = 'enrolled'
            ORDER BY s.day_time
            """;
        
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("section_id", rs.getInt("section_id"));
                    row.put("course_code", rs.getString("course_code"));
                    row.put("course_title", rs.getString("course_title"));
                    row.put("day_time", rs.getString("day_time"));
                    row.put("room", rs.getString("room"));
                    row.put("instructor", rs.getString("instructor"));
                    out.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return out;
    }
}