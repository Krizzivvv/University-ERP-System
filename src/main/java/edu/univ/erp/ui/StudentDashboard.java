package edu.univ.erp.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import edu.univ.erp.db.DBManager;
import edu.univ.erp.model.AuthUser;
import edu.univ.erp.model.CourseSection;
import edu.univ.erp.service.CatalogService;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.util.LoadingDialog;
import edu.univ.erp.util.SettingsDao;
import edu.univ.erp.util.TranscriptExporter;
import net.miginfocom.swing.MigLayout;

public class StudentDashboard extends JFrame {
    
    private static final Color BACKGROUND = new Color(245, 239, 221);      // Light sepia background
    private static final Color PANEL = new Color(231, 220, 197);           // Deeper sepia for panels
    private static final Color PRIMARY = new Color(46, 79, 79);            // Wine green for primary actions
    private static final Color ACCENT = new Color(158, 179, 132);          // Sage green for accents
    private static final Color TEXT_PRIMARY = new Color(62, 39, 35);       // Coffee brown for main text
    private static final Color TEXT_SECONDARY = new Color(93, 64, 55);     // Softer brown for secondary text
    private static final Color SUCCESS = new Color(34, 139, 34);           // Success green
    private static final Color ERROR = new Color(165, 42, 42);             // Error red
    private static final Color HOVER = new Color(198, 209, 172);           // Sage hover

    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 24);
    private static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 20);
    private static final Font FONT_SUB = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONT_BUTTON = new Font("SansSerif", Font.BOLD, 13);
    private static final Font FONT_NAV = new Font("SansSerif", Font.PLAIN, 14);

    private static final int SIDEBAR_WIDTH = 220;
    
    private final AuthUser user;                    
    private JPanel mainContentPanel;
    private CardLayout cardLayout;                  
    private JButton selectedNavButton = null;       
    
    private JButton btnRegisteredCourses;           
    private JButton btnRegistration;                
    private JButton btnTimetable;                   
    private JButton btnGrades;                      
    private JButton btnChangePassword;              
    private JButton btnTranscript;                 

    private DefaultTableModel registeredCoursesModel; 
    private JTable registeredCoursesTable;            
    private JTextField searchField;                 
    private List<CourseSection> allSections;        
    
    private DefaultTableModel registrationModel;   
    private JTable registrationTable;               

    private DefaultTableModel gradesModel;         
    private JTable gradesTable;                     
    private JLabel lblGpaSummary;                   
    
    public StudentDashboard(AuthUser user) {
        this.user = user;
        this.allSections = new ArrayList<>();
        
        setTitle("ERP Portal - Student");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 800);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);
        
        initializeUI();
        
        loadAllData();
        handleNavigation("registered_courses", btnRegisteredCourses);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));

        add(createTopBar(), BorderLayout.NORTH);            
        add(createSideNavigation(), BorderLayout.WEST);     
        add(createMainContent(), BorderLayout.CENTER);      
    }
    
    private JPanel createTopBar() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        headerPanel.setPreferredSize(new Dimension(0, 70));
        
        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new BoxLayout(welcomePanel, BoxLayout.Y_AXIS));
        welcomePanel.setOpaque(false);

        JLabel welcomeLabel = new JLabel("Welcome, " + user.getUsername());
        welcomeLabel.setFont(FONT_HEADER);
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel roleLabel = new JLabel("Student Portal");
        roleLabel.setFont(FONT_SUB);
        roleLabel.setForeground(ACCENT);
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        welcomePanel.add(welcomeLabel);
        welcomePanel.add(Box.createVerticalStrut(4));
        welcomePanel.add(roleLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);

        JButton logoutButton = createStyledButton("Logout", ERROR, Color.WHITE);
        logoutButton.setPreferredSize(new Dimension(110, 38));
        logoutButton.addActionListener(e -> handleLogout());

        rightPanel.add(logoutButton);

        headerPanel.add(welcomePanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private JPanel createSideNavigation() {
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(PANEL);
        navPanel.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        navPanel.setBorder(BorderFactory.createEmptyBorder(18, 14, 18, 14));

        JLabel navTitle = new JLabel("ERP Portal");
        navTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        navTitle.setForeground(PRIMARY);
        navTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        navPanel.add(navTitle);
        navPanel.add(Box.createVerticalStrut(20));
        
        btnRegisteredCourses = createNavButton("Registered Courses", "registered_courses");
        btnRegistration = createNavButton("Course Registration", "registration");
        btnTimetable = createNavButton("View Timetable", "timetable");
        btnGrades = createNavButton("Grades", "grades");
        btnChangePassword = createNavButton("Change Password", null); // Special case
        btnTranscript = createNavButton("Download Transcript", "transcript");
        
        addNavButtonToPanel(navPanel, btnRegisteredCourses);
        addNavButtonToPanel(navPanel, btnRegistration);
        addNavButtonToPanel(navPanel, btnTimetable);
        addNavButtonToPanel(navPanel, btnGrades);
        addNavButtonToPanel(navPanel, btnChangePassword);
        addNavButtonToPanel(navPanel, btnTranscript);

        for(java.awt.event.ActionListener al : btnChangePassword.getActionListeners()) {
            btnChangePassword.removeActionListener(al);
        }
        btnChangePassword.addActionListener(e -> handleChangePassword());

        navPanel.add(Box.createVerticalGlue());

        try {
            SettingsDao settingsDao = new SettingsDao();
            if (settingsDao.getBoolean("maintenance_on", false)) {
                JLabel maintLabel = new JLabel("MAINTENANCE ON");
                maintLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
                maintLabel.setForeground(ERROR);
                maintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                navPanel.add(maintLabel);
                navPanel.add(Box.createVerticalStrut(10));
            }
        } catch (Exception e) {
        }

        return navPanel;
    }

    private void addNavButtonToPanel(JPanel panel, JButton btn) {
        panel.add(btn);
        panel.add(Box.createVerticalStrut(8));
    }

    private JButton createNavButton(String text, String viewName) {
        JButton button = new JButton(text);
        button.setFont(FONT_NAV);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(SIDEBAR_WIDTH - 24, 42));
        button.setPreferredSize(new Dimension(SIDEBAR_WIDTH - 24, 42));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        styleNavDeselected(button);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button != selectedNavButton) button.setBackground(HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button != selectedNavButton) button.setBackground(PANEL);
            }
        });

        if (viewName != null) {
            button.addActionListener(e -> handleNavigation(viewName, button));
        }
        
        return button;
    }

    private void handleNavigation(String view, JButton clickedButton) {
        if (clickedButton == selectedNavButton) return;

        if (selectedNavButton != null) {
            styleNavDeselected(selectedNavButton);
        }

        selectedNavButton = clickedButton;
        styleNavSelected(selectedNavButton);
        
        cardLayout.show(mainContentPanel, view);
        
        switch (view) {
            case "registration": loadRegistration(); break;
            case "grades": loadGrades(); break;
            case "registered_courses": loadRegisteredCourses(); break;
        }
    }

    private void styleNavSelected(JButton btn) {
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
    }

    private void styleNavDeselected(JButton btn) {
        btn.setBackground(PANEL);
        btn.setForeground(TEXT_PRIMARY);
    }
    
    private JPanel createMainContent() {
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(BACKGROUND);
        
        mainContentPanel.add(createRegisteredCoursesPanel(), "registered_courses");
        mainContentPanel.add(createRegistrationPanel(), "registration");
        mainContentPanel.add(createTimetablePanel(), "timetable");
        mainContentPanel.add(createGradesPanel(), "grades");
        mainContentPanel.add(createTranscriptPanel(), "transcript");
        
        return mainContentPanel;
    }
    
    // REGISTERED COURSES
    private JPanel createRegisteredCoursesPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 20", "[grow]", "[]15[]15[grow]"));
        panel.setBackground(BACKGROUND);
        
        JLabel title = createTitleLabel("My Registered Courses");
        panel.add(title, "wrap");
        
        JPanel controlsPanel = new JPanel(new MigLayout("fill, insets 10", "[]10[]10[300!]10[]10[]push", "[]"));
        controlsPanel.setBackground(PANEL);
        controlsPanel.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        
        JButton btnRefresh = createStyledButton("Refresh", PRIMARY, BACKGROUND);
        btnRefresh.addActionListener(e -> loadRegisteredCourses());
        controlsPanel.add(btnRefresh);

        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setForeground(TEXT_PRIMARY);
        controlsPanel.add(lblSearch);
        
        searchField = new JTextField();
        styleTextField(searchField);
        searchField.addActionListener(e -> filterRegisteredCourses());
        controlsPanel.add(searchField, "growx");
        
        JButton btnSearch = createStyledButton("Search", PRIMARY, BACKGROUND);
        btnSearch.addActionListener(e -> filterRegisteredCourses());
        controlsPanel.add(btnSearch);
        
        JButton btnSortAZ = createStyledButton("Sort A-Z", ACCENT, TEXT_PRIMARY);
        btnSortAZ.addActionListener(e -> sortRegisteredCoursesAlphabetically());
        controlsPanel.add(btnSortAZ);
        
        panel.add(controlsPanel, "growx, wrap");
        
        String[] columns = {"Section ID", "Course Code", "Course Title", "Day/Time", "Room", "Instructor", "Semester", "Year"};
        registeredCoursesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        registeredCoursesTable = new JTable(registeredCoursesModel);
        styleTable(registeredCoursesTable);
        
        JScrollPane scrollPane = new JScrollPane(registeredCoursesTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        scrollPane.getViewport().setBackground(BACKGROUND);
        panel.add(scrollPane, "grow");
        
        return panel;
    }
    
    // Course REGISTRATION 
    private JPanel createRegistrationPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 20", "[grow]", "[]15[]15[grow]15[]"));
        panel.setBackground(BACKGROUND);
        
        JLabel title = createTitleLabel("Course Registration");
        panel.add(title, "wrap");

        JPanel btnPanel = new JPanel(new MigLayout("", "[]10[]10[]push", "[]"));
        btnPanel.setBackground(BACKGROUND);
        
        JButton btnAdd = createStyledButton("Add to Schedule", ACCENT, Color.WHITE);
        btnPanel.add(btnAdd);
        
        JButton btnDrop = createStyledButton("Drop from Schedule", ACCENT, Color.WHITE);
        btnPanel.add(btnDrop);
        
        JButton btnRefresh = createStyledButton("Refresh",ACCENT, BACKGROUND);
        btnPanel.add(btnRefresh);
        
        panel.add(btnPanel, "growx, wrap");
        
        btnAdd.addActionListener(e -> handleAddCourse());
        btnDrop.addActionListener(e -> handleDropCourse());
        btnRefresh.addActionListener(e -> loadRegistration());
        
        String[] columns = {"Section ID", "Course", "Title", "Day/Time", "Room", "Instructor", "Seats", "Status", "Drop Deadline"};
        registrationModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        registrationTable = new JTable(registrationModel);
        styleTable(registrationTable);
        
        btnAdd.setEnabled(false);
        btnDrop.setEnabled(false);
        
        registrationTable.getSelectionModel().addListSelectionListener(ev -> {
            boolean has = registrationTable.getSelectedRow() >= 0;
            btnAdd.setEnabled(has);
            btnDrop.setEnabled(has);
        });

        
        JScrollPane scrollPane = new JScrollPane(registrationTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        scrollPane.getViewport().setBackground(BACKGROUND);
        panel.add(scrollPane, "grow, push,wrap");
        
        return panel;
    }

    
    // TIMETABLE PANEL
    private JPanel createTimetablePanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 20", "[grow]", "[]15[grow]"));
        panel.setBackground(BACKGROUND);
        
        JLabel title = createTitleLabel("My Timetable");
        panel.add(title, "wrap");
        
        try {
            TimetablePanel timetablePanel = new TimetablePanel(user.getUserId());
            timetablePanel.setBackground(BACKGROUND);
            panel.add(timetablePanel, "grow");
        } catch (Exception e) {
            JLabel errorLabel = new JLabel("Timetable feature unavailable. Please contact support.");
            errorLabel.setForeground(ERROR);
            panel.add(errorLabel, "grow");
        }
        
        return panel;
    }
    
    // GRADES PANEL 
    private JPanel createGradesPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 20", "[grow]", "[]15[]15[grow]"));
        panel.setBackground(BACKGROUND);
        
        JLabel title = createTitleLabel("My Grades");
        panel.add(title, "wrap");
        
        JPanel gpaPanel = new JPanel(new MigLayout("fill, insets 15", "[]push[]10[]", "[]"));
        gpaPanel.setBackground(PRIMARY);
        gpaPanel.setBorder(BorderFactory.createLineBorder(ACCENT, 2));
        
        JLabel lblGpaTitle = new JLabel("Academic Performance:");
        lblGpaTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblGpaTitle.setForeground(BACKGROUND);
        gpaPanel.add(lblGpaTitle);
        
        lblGpaSummary = new JLabel("GPA: -   (Weighted: -)");
        lblGpaSummary.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblGpaSummary.setForeground(ACCENT);
        gpaPanel.add(lblGpaSummary);
        
        JButton btnRefresh = createStyledButton("Refresh", ACCENT, TEXT_PRIMARY);
        btnRefresh.addActionListener(e -> loadGrades());
        gpaPanel.add(btnRefresh);
        
        panel.add(gpaPanel, "growx, wrap");
        
        String[] columns = {"Course", "Section", "Component", "Score", "Max", "Weight (%)", "Percent", "Grade"};
        gradesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        gradesTable = new JTable(gradesModel);
        styleTable(gradesTable);
        
        JScrollPane scrollPane = new JScrollPane(gradesTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        scrollPane.getViewport().setBackground(BACKGROUND);
        panel.add(scrollPane, "grow");
        
        return panel;
    }
    
    private void handleChangePassword() {
        ChangePasswordDialog dialog = new ChangePasswordDialog(this, user.getUserId());
        dialog.setVisible(true);
    }
    
    // TRANSCRIPT PANEL 
    private JPanel createTranscriptPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 20", "[grow]", "[]30[grow]"));
        panel.setBackground(BACKGROUND);
        
        JLabel title = createTitleLabel("Download Transcript");
        panel.add(title, "wrap");
        
        JPanel contentPanel = new JPanel(new MigLayout("fill, insets 40", "[grow,center]", "[][][]30[]"));
        contentPanel.setBackground(PANEL);
        contentPanel.setBorder(BorderFactory.createLineBorder(ACCENT, 2));
        
        JLabel lblDesc = new JLabel("Export your complete academic transcript");
        lblDesc.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblDesc.setForeground(TEXT_PRIMARY);
        lblDesc.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(lblDesc, "wrap");
        
        JLabel lblInfo = new JLabel("Download your transcript as a CSV file for records and applications");
        lblInfo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblInfo.setForeground(TEXT_SECONDARY);
        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(lblInfo, "wrap");
        
        JButton btnExport = createStyledButton("Export Transcript (CSV)", PRIMARY, BACKGROUND);
        btnExport.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnExport.addActionListener(e -> handleExportTranscript());
        contentPanel.add(btnExport, "width 250!");
        
        panel.add(contentPanel, "grow");
        
        return panel;
    }
    
    // STYLING HELPER 
    private JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_TITLE);
        label.setForeground(PRIMARY);
        return label;
    }
    
    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        
        btn.addMouseListener(new MouseAdapter() {
            Color originalBg = bg;
            public void mouseEntered(MouseEvent evt) {
                btn.setBackground(bg.darker());
            }
            public void mouseExited(MouseEvent evt) {
                btn.setBackground(originalBg);
            }
        });
        
        return btn;
    }
    
    private void styleTextField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(BACKGROUND);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1),
            new EmptyBorder(5, 8, 5, 8)
        ));
    }
    
    private void styleTable(JTable table) {
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(BACKGROUND);
        table.setSelectionBackground(ACCENT);
        table.setSelectionForeground(BACKGROUND);
        table.setRowHeight(28);
        table.setGridColor(new Color(231, 220, 197, 100));
        table.setShowGrid(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setForeground(BACKGROUND);
        table.getTableHeader().setBackground(PRIMARY);
        table.getTableHeader().setBorder(BorderFactory.createLineBorder(ACCENT));
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }
    
    // DATA LOADING 
    private void loadAllData() {
        loadRegisteredCourses();
    }
    
    private void loadRegisteredCourses() {
        LoadingDialog.showLoading(this);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                registeredCoursesModel.setRowCount(0);
                
                String sql = """
                    SELECT s.section_id, c.code AS course_code, c.title AS course_title,
                        s.day_time, s.room, s.semester, s.year,
                        ua.username AS instructor_name
                    FROM enrollments e
                    JOIN sections s ON e.section_id = s.section_id
                    JOIN courses c ON s.course_id = c.course_id
                    LEFT JOIN univ_auth.users_auth ua ON ua.user_id = s.instructor_id
                    WHERE e.student_id = ? AND e.status = 'enrolled'
                    ORDER BY c.code
                    """;
                
                try (Connection conn = DBManager.getErpConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                    
                    ps.setInt(1, user.getUserId());
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String instructorName = rs.getString("instructor_name");
                            if (instructorName == null || instructorName.isEmpty()) {
                                instructorName = "Not Assigned";
                            }
                            
                            registeredCoursesModel.addRow(new Object[]{
                                rs.getInt("section_id"),
                                rs.getString("course_code"),
                                rs.getString("course_title"),
                                rs.getString("day_time"),
                                rs.getString("room"),
                                instructorName,
                                rs.getString("semester"),
                                rs.getInt("year")
                            });
                        }
                    }
                }
                allSections = CatalogService.listAllSections();
                
                SwingUtilities.invokeLater(() -> searchField.setText(""));
                return null;
            }

            @Override
            protected void done() {
                LoadingDialog.hideLoading();
            }
        };
        worker.execute();
    }
    private String getInstructorName(Integer instructorId) {
        if (instructorId == null) return "Not Assigned";
        String sql = "SELECT username FROM univ_auth.users_auth WHERE user_id = ?";
        try (Connection conn = DBManager.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("username");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown";
    }
    
    private void filterRegisteredCourses() {
        LoadingDialog.showLoading(this);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String searchTerm = searchField.getText().toLowerCase().trim();
                registeredCoursesModel.setRowCount(0);
                
                String sql = """
                    SELECT s.section_id, c.code AS course_code, c.title AS course_title,
                        s.day_time, s.room, s.semester, s.year,
                        ua.username AS instructor_name
                    FROM enrollments e
                    JOIN sections s ON e.section_id = s.section_id
                    JOIN courses c ON s.course_id = c.course_id
                    LEFT JOIN univ_auth.users_auth ua ON ua.user_id = s.instructor_id
                    WHERE e.student_id = ? AND e.status = 'enrolled'
                    ORDER BY c.code
                    """;
                
                try (Connection conn = DBManager.getErpConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                    
                    ps.setInt(1, user.getUserId());
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String courseCode = rs.getString("course_code");
                            String courseTitle = rs.getString("course_title");
                            String room = rs.getString("room");
                            String semester = rs.getString("semester");
                            String instructorName = rs.getString("instructor_name");
                            
                            if (instructorName == null) instructorName = "Not Assigned";
                            
                            String searchable = (courseCode + " " + courseTitle + " " + 
                                            room + " " + semester).toLowerCase();
                            
                            if (searchTerm.isEmpty() || searchable.contains(searchTerm)) {
                                registeredCoursesModel.addRow(new Object[]{
                                    rs.getInt("section_id"),
                                    courseCode,
                                    courseTitle,
                                    rs.getString("day_time"),
                                    room,
                                    instructorName,
                                    semester,
                                    rs.getInt("year")
                                });
                            }
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                LoadingDialog.hideLoading();
            }
        };
        worker.execute();
    }
    private void sortRegisteredCoursesAlphabetically() {
        allSections.sort(Comparator.comparing(CourseSection::getCourseCode)
                                    .thenComparing(CourseSection::getCourseTitle));
        filterRegisteredCourses();
    }
    
    private void loadRegistration() {
        LoadingDialog.showLoading(this);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                registrationModel.setRowCount(0);
                
                Set<Integer> enrolledCourseIds = new java.util.HashSet<>();
                Set<Integer> enrolledSectionIds = new java.util.HashSet<>();
                
                String enrolledSql = """
                    SELECT DISTINCT s.course_id, e.section_id
                    FROM enrollments e
                    JOIN sections s ON e.section_id = s.section_id
                    WHERE e.student_id = ? AND e.status = 'enrolled'
                    """;
                
                try (Connection conn = DBManager.getErpConnection();
                    PreparedStatement ps = conn.prepareStatement(enrolledSql)) {
                    ps.setInt(1, user.getUserId());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            enrolledCourseIds.add(rs.getInt("course_id"));
                            enrolledSectionIds.add(rs.getInt("section_id"));
                        }
                    }
                }

                List<CourseSection> sections = CatalogService.listAllSections();
                for (CourseSection cs : sections) {
                    String instructorName = getInstructorName(cs.getInstructorId());
                    int seatsLeft = cs.getCapacity() - cs.getEnrolled();
                    String seatsInfo = seatsLeft + " seats available";
                    
                    String status;
                    if (enrolledSectionIds.contains(cs.getSectionId())) {
                        status = "ENROLLED"; // Currently enrolled in THIS section
                    } else if (enrolledCourseIds.contains(cs.getCourseId())) {
                        status = "*ENROLLED"; // Enrolled in different section of same course
                    } else if (seatsLeft <= 0) {
                        status = "CLOSE";
                    } else {
                        status = "OPEN";
                    }
                    
                    String dropDeadline = "available soon...";
                    registrationModel.addRow(new Object[]{
                        cs.getSectionId(), cs.getCourseCode(), cs.getCourseTitle(), cs.getDayTime(),
                        cs.getRoom(), instructorName, seatsInfo, status, dropDeadline
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                LoadingDialog.hideLoading();
            }
        };
        worker.execute();
    }
    
    private void loadGrades() {
        LoadingDialog.showLoading(this);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
             @Override
            protected Void doInBackground() throws Exception {
                gradesModel.setRowCount(0);
                try {
                    List<Map<String, Object>> rows = StudentService.listGradesForStudent(user.getUserId());
                    if (rows == null || rows.isEmpty()) {
                        lblGpaSummary.setText("GPA: -");
                        lblGpaSummary.setForeground(TEXT_SECONDARY);
                        return null;
                    }

                    // course + section + status
                    Map<String, List<Map<String,Object>>> groups = new java.util.LinkedHashMap<>();
                    for (Map<String,Object> r : rows) {
                        String courseCode = String.valueOf(r.getOrDefault("course_code", ""));
                        String courseTitle = String.valueOf(r.getOrDefault("course_title", ""));
                        Object sectionObj = r.getOrDefault("section_label", "");
                        String sectionLabel = String.valueOf(sectionObj);
                        String status = String.valueOf(r.getOrDefault("enrollment_status", "enrolled"));
                        String key = courseCode + " - " + courseTitle + " (Section " + sectionLabel + ") [" + status + "]";
                        groups.putIfAbsent(key, new ArrayList<>());
                        groups.get(key).add(r);
                    }

                    double totalGradePoints = 0.0;
                    int courseCount = 0;

                    for (Map.Entry<String, List<Map<String,Object>>> entry : groups.entrySet()) {
                        String groupKey = entry.getKey(); // contains status too
                        List<Map<String,Object>> groupRows = entry.getValue();

                        String status = String.valueOf(groupRows.get(0).getOrDefault("enrollment_status", "enrolled"));

                        if ("dropped".equalsIgnoreCase(status)) {
                            gradesModel.addRow(new Object[]{
                                groupKey + " - DROPPED",
                                "",
                                "Dropped - no grades", 
                                "-", "-", "-", "-", "-" 
                            });
                            gradesModel.addRow(new Object[]{"", "", "", "", "", "", "", ""});
                            continue;
                        }

                        double currentCourseWeightedSum = 0.0;
                        double totalWeightGraded = 0.0;
                        for (Map<String,Object> r : groupRows) {
                            String component = String.valueOf(r.getOrDefault("component", ""));
                            if ("FINAL_TOTAL".equals(component) || "FINAL_LETTER".equals(component)) continue;
                            Object scoreObj = r.get("score");
                            Object maxScoreObj = r.get("max_score");
                            Object weightObj = r.get("weight_pct");
                            double maxScore = maxScoreObj != null ? ((Number) maxScoreObj).doubleValue() : 100.0;
                            double weight = weightObj != null ? ((Number) weightObj).doubleValue() : 0.0;
                            String displayScore, displayPercent, displayGrade;
                            if (scoreObj == null) {
                                displayScore = "-"; displayPercent = "-"; displayGrade = "Pending";
                            } else {
                                double score = ((Number) scoreObj).doubleValue();
                                double rawPercentage = maxScore > 0 ? (score / maxScore) * 100.0 : 0.0;
                                double weightedContribution = maxScore > 0 ? (score / maxScore) * weight : 0.0;
                                currentCourseWeightedSum += weightedContribution;
                                totalWeightGraded += weight;
                                displayScore = String.format("%.2f", score);
                                displayPercent = String.format("%.2f%%", rawPercentage);
                                displayGrade = calculateGrade(rawPercentage);
                            }
                            gradesModel.addRow(new Object[]{
                                groupKey, String.valueOf(r.getOrDefault("section_label","")), component,
                                displayScore, String.format("%.2f", maxScore), String.format("%.2f", weight),
                                displayPercent, displayGrade
                            });
                        }

                        String finalGrade = (totalWeightGraded > 0) ? calculateGrade(currentCourseWeightedSum) : "-";
                        gradesModel.addRow(new Object[]{groupKey + " - TOTAL", "", "TOTAL",
                            String.format("%.2f", currentCourseWeightedSum), "100.00", "100.00",
                            String.format("%.2f%%", currentCourseWeightedSum), finalGrade
                        });

                        if (totalWeightGraded > 0) {
                            totalGradePoints += currentCourseWeightedSum;
                            courseCount++;
                        }
                        gradesModel.addRow(new Object[]{"", "", "", "", "", "", "", ""});
                    }

                    if (courseCount > 0) {
                        double avgScore = totalGradePoints / courseCount;
                        double avgGPA = avgScore / 10.0;
                        lblGpaSummary.setText(String.format("GPA: %.2f", avgGPA));
                        if (avgGPA >= 3.5) lblGpaSummary.setForeground(SUCCESS);
                        else if (avgGPA >= 2.5) lblGpaSummary.setForeground(ACCENT);
                        else lblGpaSummary.setForeground(ERROR);
                    } else {
                        lblGpaSummary.setText("GPA: - ");
                        lblGpaSummary.setForeground(TEXT_SECONDARY);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    lblGpaSummary.setText("Error loading grades");
                    lblGpaSummary.setForeground(ERROR);
                }

                return null;
            }

                    @Override
                    protected void done() {
                        LoadingDialog.hideLoading();
                    }
            };

        worker.execute();
    }
    

    private String calculateGrade(double percentage) {
        if (percentage >= 95) return "A+";
        if (percentage >= 90) return "A";
        if (percentage >= 85) return "A-";
        if (percentage >= 80) return "B+";
        if (percentage >= 75) return "B";
        if (percentage >= 70) return "B-";
        if (percentage >= 65) return "C+";
        if (percentage >= 60) return "C";
        if (percentage >= 55) return "C-";
        if (percentage >= 50) return "D";
        return "F";
    }
    
    private void handleAddCourse() {
        int viewRow = registrationTable.getSelectedRow();
        if (viewRow < 0) {
            showMessage("Please select a course section to add.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = registrationTable.convertRowIndexToModel(viewRow);

        Object secObj = registrationModel.getValueAt(modelRow, 0);
        if (!(secObj instanceof Number)) {
            showMessage("Invalid section selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int sectionId = ((Number) secObj).intValue();
        String status = String.valueOf(registrationModel.getValueAt(modelRow, 7));

        // CHECK STATUS BEFORE PROCEEDING  
        if ("FULL".equalsIgnoreCase(status)) {
            showMessage("This section is full. Please choose another section.", "Section Full", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (status != null && status.startsWith("ENROLLED")) {
            showMessage("You are already enrolled in this course.\n" +
                        "Please drop your current enrollment first if you want to switch sections.",
                        "Already Enrolled", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        LoadingDialog.showLoading(this);
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return StudentService.registerForSection(user.getUserId(), sectionId);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                                        if (result != null && result.startsWith("ALREADY_ENROLLED_IN_COURSE:")) {
                        String courseInfo = result.substring("ALREADY_ENROLLED_IN_COURSE:".length());
                        showMessage(
                            "You are already enrolled in:\n" + courseInfo + "\n\n" +
                            "You cannot register for multiple sections of the same course.\n" +
                            "Please drop the existing section first if you want to switch.",
                            "Already Enrolled in This Course",
                            JOptionPane.WARNING_MESSAGE
                        );
                        LoadingDialog.hideLoading();
                        return;
                    }
                    
                    if ("OK".equals(result)) {
                        showMessage("Successfully registered for the course!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadRegistration();
                        loadRegisteredCourses(); // REFRESH REGISTERED COURSES
                        loadGrades();
                    } else if ("DUPLICATE".equals(result)) {
                        showMessage("You are already enrolled in this section.", "Already Enrolled", JOptionPane.WARNING_MESSAGE);
                    } else if ("FULL".equals(result)) {
                        showMessage("This section is now full.", "Section Full", JOptionPane.WARNING_MESSAGE);
                    } else if ("MAINTENANCE".equals(result)) {
                        showMessage("Registration is temporarily disabled due to maintenance.", "Maintenance", JOptionPane.ERROR_MESSAGE);
                    } else if ("NOT_OPEN_YET".equals(result)) {
                        showMessage("Registration for this section has not opened yet.", "Not Open", JOptionPane.WARNING_MESSAGE);
                    } else if ("REGISTRATION_CLOSED".equals(result)) {
                        showMessage("Registration for this section has closed.", "Registration Closed", JOptionPane.WARNING_MESSAGE);
                    } else {
                        showMessage("Registration failed: " + result, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showMessage("Registration failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    private void handleDropCourse() {
        int viewRow = registrationTable.getSelectedRow();
        if (viewRow < 0) {
            showMessage("Please select a course section to drop.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = registrationTable.convertRowIndexToModel(viewRow);

        Object secObj = registrationModel.getValueAt(modelRow, 0);
        if (!(secObj instanceof Number)) {
            showMessage("Invalid section selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int sectionId = ((Number) secObj).intValue();
        String status = String.valueOf(registrationModel.getValueAt(modelRow, 7));

        if (status == null || !status.equalsIgnoreCase("ENROLLED")) {
            showMessage("You are not enrolled in this section.\n" +
                        "You can only drop courses you are currently enrolled in.\n\n" +
                        "Note: ENROLLED* means you're enrolled in a different section of this course.",
                        "Not Enrolled", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to drop this course?",
            "Confirm Drop",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        LoadingDialog.showLoading(this);
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return StudentService.dropEnrollmentSafe(user.getUserId(), sectionId);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    
                    if ("OK".equals(result)) {
                        showMessage("Successfully dropped the course!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadRegistration();
                        loadRegisteredCourses(); // REFRESH REGISTERED COURSES
                        loadGrades();
                    } else if ("NOT_ENROLLED".equals(result)) {
                        showMessage("You are not enrolled in this section.", "Not Enrolled", JOptionPane.WARNING_MESSAGE);
                    } else if ("AFTER_DEADLINE".equals(result)) {
                        showMessage("Drop deadline has passed for this course.\nPlease contact the admin office.", 
                                    "Deadline Passed", JOptionPane.ERROR_MESSAGE);
                    } else if ("MAINTENANCE".equals(result)) {
                        showMessage("Drop is temporarily disabled due to maintenance.", "Maintenance", JOptionPane.ERROR_MESSAGE);
                    } else {
                        showMessage("Drop failed: " + result, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showMessage("Drop failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    private void handleExportTranscript() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Transcript as CSV");
        fc.setSelectedFile(new File("transcript_" + user.getUserId() + ".csv"));
        int result = fc.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File outputFile = fc.getSelectedFile();

        LoadingDialog.showLoading(this);
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            Exception error;
            @Override
            protected Boolean doInBackground() {
                try {
                    TranscriptExporter.exportCsvForStudent(user.getUserId(), outputFile);
                    return true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    error = ex;
                    return false;
                }
            }
            @Override
            protected void done() {
                try {
                    if (get()) showMessage("Transcript exported successfully to:\n" + outputFile.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    else showMessage("Failed to export transcript: " + (error != null ? error.getMessage() : "Unknown error"), "Export Failed", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    showMessage("Export error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }
    
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
    
    private void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }
}