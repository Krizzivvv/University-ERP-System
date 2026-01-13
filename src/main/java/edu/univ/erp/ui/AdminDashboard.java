package edu.univ.erp.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import edu.univ.erp.db.DBManager;
import edu.univ.erp.model.AuthUser;
import edu.univ.erp.service.CourseService;
import edu.univ.erp.service.SectionService;
import edu.univ.erp.service.UserService;
import edu.univ.erp.util.LoadingDialog;
import edu.univ.erp.util.SettingsDao;


public class AdminDashboard extends JFrame {
   
    private static final Color BACKGROUND = new Color(245, 239, 221);      // Light sepia background
    private static final Color PANEL = new Color(231, 220, 197);           // Deeper sepia for panels
    private static final Color PRIMARY = new Color(46, 79, 79);            // Wine green for primary actions
    private static final Color ACCENT = new Color(158, 179, 132);          // Sage green for accents
    private static final Color TEXT_PRIMARY = new Color(62, 39, 35);       // Coffee brown for main text
    private static final Color ERROR = new Color(165, 42, 42);             // Error red
    private static final Color HOVER = new Color(198, 209, 172);

    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 24);
    private static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 20);
    private static final Font FONT_SUB = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONT_BUTTON = new Font("SansSerif", Font.BOLD, 13);
    private static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);

    private static final int SIDEBAR_WIDTH = 220;
 
    private final AuthUser user;                    
    private final SettingsDao settingsDao;          
    
    private JPanel mainContentPanel;                
    private CardLayout cardLayout;                  
    private JButton selectedNavButton = null;       
    private JCheckBox maintenanceCheckbox;        

    private DefaultTableModel coursesModel;         
    private JTable coursesTable;                   

    private DefaultTableModel sectionsModel;       
    private JTable sectionsTable;                   

    private DefaultTableModel usersModel;           
    private JTable usersTable;        
    
    private DefaultTableModel backupsModel;
    private JTable backupsTable;

    private static final String MYSQL_BIN_PATH = "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\";
    private static final String BACKUP_DIR = System.getProperty("user.dir") + java.io.File.separator + "backups";

    public AdminDashboard(AuthUser user) {
        this.user = user;
        this.settingsDao = new SettingsDao();
        
        setTitle("ERP Portal - Admin");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1400, 800);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);
        new java.io.File(BACKUP_DIR).mkdirs();

        initializeUI();
    }

    //    UI INITIALIZATION   
    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));

        // Create main components
        add(createTopBar(), BorderLayout.NORTH);           // Top header with welcome message
        add(createnavPaneligation(), BorderLayout.WEST);    // Left sidebar navigation
        add(createMainContent(), BorderLayout.CENTER);     // Center content area

        loadCoursesData();
        showPanel("courses");
    }

    //    TOP BAR   
    private JPanel createTopBar() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        headerPanel.setPreferredSize(new Dimension(0, 70));
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));


        // Left side - Welcome message
        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new BoxLayout(welcomePanel, BoxLayout.Y_AXIS));
        welcomePanel.setOpaque(false);

        JLabel welcomeLabel = new JLabel("Welcome, " + user.getUsername());
        welcomeLabel.setFont(FONT_HEADER);
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        welcomeLabel.getAccessibleContext().setAccessibleName("WelcomeLabel");

        JLabel roleLabel = new JLabel("Admin");
        roleLabel.setFont(FONT_SUB);
        roleLabel.setForeground(ACCENT);
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        welcomePanel.add(welcomeLabel);
        welcomePanel.add(Box.createVerticalStrut(6));
        welcomePanel.add(roleLabel);

        // Right side 
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setOpaque(false);

        // Maintenance Mode Toggle
        JPanel maintenancePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        maintenancePanel.setOpaque(false);

        JLabel maintenanceLabel = new JLabel("Maintenance Mode");
        maintenanceLabel.setFont(FONT_SUB);
        maintenanceLabel.setForeground(Color.WHITE);

        maintenanceCheckbox = new JCheckBox();
        maintenanceCheckbox.setOpaque(false);
        maintenanceCheckbox.setFocusPainted(false);
        maintenanceCheckbox.setSelected(settingsDao.getBoolean("maintenance_on", false));
        maintenanceCheckbox.addActionListener(e -> toggleMaintenance());

        maintenancePanel.add(maintenanceLabel);
        maintenancePanel.add(maintenanceCheckbox);

        // Logout Button
        JButton logoutButton = createStyledButton("Logout", ERROR, Color.WHITE);
        logoutButton.setPreferredSize(new Dimension(110, 38));
        logoutButton.setToolTipText("Sign out of the ERP system");
        logoutButton.addActionListener(e -> handleLogout());

        rightPanel.add(maintenancePanel);
        rightPanel.add(logoutButton);

        headerPanel.add(welcomePanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    //    LEFT NAVIGATION PANEL   
    private JPanel createnavPaneligation() {
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
        navPanel.add(Box.createVerticalStrut(8));

        // Buttons
        navPanel.add(createNavButton("Courses", "courses", true));
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(createNavButton("Sections", "sections", false));
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(createNavButton("Users", "users", false));
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(createNavButton("Backups", "backups", false));
        navPanel.add(Box.createVerticalStrut(8)); 
        JButton btnBulkReg = createNavButton("Registration Windows", "bulk-reg", false);
        btnBulkReg.addActionListener(e -> {
            new BulkRegistrationDialog(AdminDashboard.this).setVisible(true);
            loadSectionsData(); 
        });
        navPanel.add(btnBulkReg);
        navPanel.add(Box.createVerticalStrut(8));

        JButton btnChangePassword = createNavButton("Change Password", "change-password", false);
        btnChangePassword.addActionListener(e -> handleChangePassword());
        navPanel.add(btnChangePassword);
        navPanel.add(Box.createVerticalStrut(8));

        return navPanel;
    }

    private JButton createNavButton(String text, String view, boolean selected) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 14));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(SIDEBAR_WIDTH - 24, 42));
        button.setPreferredSize(new Dimension(SIDEBAR_WIDTH - 24, 42));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (selected) {
            styleNavSelected(button);
            selectedNavButton = button;
        } else {
            styleNavDeselected(button);
        }

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

        button.addActionListener(e -> handleNavigation(view, button));
        
        return button;
    }

    // Handles navigation button clicks
    private void handleNavigation(String view, JButton clickedButton) {
        if (clickedButton == selectedNavButton) return;

        if (selectedNavButton != null) {
            styleNavDeselected(selectedNavButton);
        }

        selectedNavButton = clickedButton;
        styleNavSelected(selectedNavButton);

        showPanel(view);
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
        mainContentPanel.add(createCoursesPanel(), "courses");
        mainContentPanel.add(createSectionsPanel(), "sections");
        mainContentPanel.add(createUsersPanel(), "users");
        mainContentPanel.add(createBackupsPanel(), "backups");
        return mainContentPanel;
    }

    //Shows a specific panel in the CardLayout
    private void showPanel(String panelName) {
        cardLayout.show(mainContentPanel, panelName);

        switch (panelName) {
            case "courses":
                loadCoursesData();
                break;
            case "sections":
                loadSectionsData();
                break;
            case "users":
                loadUsersData();
                break;
        }
    }

    //    COURSES PANEL   
    private JPanel createCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Title
        JLabel title = createTitleLabel("Course Management");
        panel.add(title, BorderLayout.NORTH);

        // Main content
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBackground(BACKGROUND);

        //Buttons
        JPanel actionsBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionsBar.setBackground(BACKGROUND);

        JButton btnAdd = createStyledButton("Add Course", ACCENT, Color.WHITE);
        JButton btnEdit = createStyledButton("Edit", ACCENT, Color.WHITE);
        JButton btnDelete = createStyledButton("Delete", ACCENT, Color.WHITE);
        JButton btnRefresh = createStyledButton("Refresh", ACCENT, Color.WHITE);

        btnAdd.addActionListener(e -> handleAddCourse());
        btnEdit.addActionListener(e -> handleEditCourse());
        btnDelete.addActionListener(e -> handleDeleteCourse());
        btnRefresh.addActionListener(e -> loadCoursesData());

        actionsBar.add(btnAdd);
        actionsBar.add(btnEdit);
        actionsBar.add(btnDelete);
        actionsBar.add(btnRefresh);

        contentPanel.add(actionsBar, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Code", "Title", "Credits", "Enrolled", "Instructor(s)"};
        coursesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        coursesTable = new JTable(coursesModel);
        styleTable(coursesTable);

        JScrollPane scrollPane = new JScrollPane(coursesTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        scrollPane.getViewport().setBackground(BACKGROUND);

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    //Loads all courses data into the table
    private void loadCoursesData() {
        LoadingDialog.showLoading(this);
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                return CourseService.listCoursesWithEnrollmentCount();
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> rows = get();
                    coursesModel.setRowCount(0);
                    if (rows != null) {
                        for (Map<String, Object> r : rows) {
                            Object instructorsObj = r.get("instructors");
                            String instructors = (instructorsObj == null ? "" : instructorsObj.toString().trim());
                            if (instructors.isEmpty()) instructors = "No instructor assigned";

                            coursesModel.addRow(new Object[]{
                                r.get("course_id"),
                                r.get("code"),
                                r.get("title"),
                                r.get("credits"),
                                r.get("enrolled"),
                                instructors
                            });
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    //    SECTIONS PANEL   
    private JPanel createSectionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Title
        JLabel title = createTitleLabel("Section Management");
        panel.add(title, BorderLayout.NORTH);

        // Main content
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBackground(BACKGROUND);

        //Buttons
        JPanel actionsBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionsBar.setBackground(BACKGROUND);

        JButton btnAdd = createStyledButton("Add Section", ACCENT, Color.WHITE);
        JButton btnEdit = createStyledButton("Edit", ACCENT, Color.WHITE);
        JButton btnDelete = createStyledButton("Delete", ACCENT, Color.WHITE);
        JButton btnRefresh = createStyledButton("Refresh", ACCENT, Color.WHITE);
        
        btnAdd.addActionListener(e -> handleAddSection());
        btnEdit.addActionListener(e -> handleEditSection());
        btnDelete.addActionListener(e -> handleDeleteSection());
        btnRefresh.addActionListener(e -> loadSectionsData());
      
        actionsBar.add(btnAdd);
        actionsBar.add(btnEdit);
        actionsBar.add(btnDelete);
        actionsBar.add(btnRefresh);

        contentPanel.add(actionsBar, BorderLayout.NORTH);

        // Table
        String[] columns = {"Section ID", "Course Code", "Course Title", "Instructor", 
                           "Day/Time", "Room", "Enrolled/Cap", "Semester", "Year", "Reg Window"};
        sectionsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        sectionsTable = new JTable(sectionsModel);
        styleTable(sectionsTable);

        JScrollPane scrollPane = new JScrollPane(sectionsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        scrollPane.getViewport().setBackground(BACKGROUND);

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private void loadSectionsData() {
        LoadingDialog.showLoading(this);
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                return listAllSectionsWithDetails();
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> sections = get();
                    sectionsModel.setRowCount(0);
                    if (sections != null) {
                        for (Map<String, Object> s : sections) {
                            String instructor = s.get("instructor_username") == null ? 
                                              "-- Unassigned --" : (String) s.get("instructor_username");
                            int enrolled = s.get("enrolled_count") == null ? 0 : (Integer) s.get("enrolled_count");
                            int capacity = s.get("capacity") == null ? 0 : (Integer) s.get("capacity");
                            Object regStartObj = s.get("registration_start");
                            Object regEndObj   = s.get("registration_end");
                            String regWindow;
                            if (regStartObj == null && regEndObj == null) {
                                regWindow = "No window (open)";
                            } else {
                                String startStr = regStartObj == null ? "-" : formatTs((Timestamp) regStartObj);
                                String endStr   = regEndObj == null ? "-" : formatTs((Timestamp) regEndObj);
                                regWindow = startStr + " - " + endStr;
                            }
                            
                            sectionsModel.addRow(new Object[]{
                                s.get("section_id"),
                                s.get("course_code"),
                                s.get("course_title"),
                                instructor,
                                s.get("day_time"),
                                s.get("room"),
                                enrolled + "/" + capacity,
                                s.get("semester"),
                                s.get("year"),
                                regWindow
                            });
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    private String formatTs(Timestamp ts) {
        if (ts == null) return "-";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy HH:mm");
        return sdf.format(new java.util.Date(ts.getTime()));
    }

    //Fetches all sections with course and enrollment details from database
    private List<Map<String, Object>> listAllSectionsWithDetails() {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = """
            SELECT s.section_id, s.course_id, c.code AS course_code, c.title AS course_title,
                   s.instructor_id, ua.username AS instructor_username,
                   s.day_time, s.room, s.capacity, s.semester, s.year,
                   s.registration_start, s.registration_end,
                   (SELECT COUNT(*) FROM enrollments e WHERE e.section_id = s.section_id 
                    AND e.status='enrolled') AS enrolled_count
            FROM sections s
            JOIN courses c ON s.course_id = c.course_id
            LEFT JOIN univ_auth.users_auth ua ON ua.user_id = s.instructor_id
            ORDER BY c.code, s.section_id
            """;
        
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("section_id", rs.getInt("section_id"));
                row.put("course_id", rs.getInt("course_id"));
                row.put("course_code", rs.getString("course_code"));
                row.put("course_title", rs.getString("course_title"));
                row.put("instructor_id", rs.getObject("instructor_id"));
                row.put("instructor_username", rs.getString("instructor_username"));
                row.put("day_time", rs.getString("day_time"));
                row.put("room", rs.getString("room"));
                row.put("capacity", rs.getInt("capacity"));
                row.put("semester", rs.getString("semester"));
                row.put("year", rs.getInt("year"));
                row.put("enrolled_count", rs.getInt("enrolled_count"));
                row.put("registration_start", rs.getTimestamp("registration_start"));
                row.put("registration_end", rs.getTimestamp("registration_end"));
                out.add(row);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return out;
    }

    //    USERS PANEL   
    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Title
        JLabel title = createTitleLabel("User Management");
        panel.add(title, BorderLayout.NORTH);

        // Main content
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBackground(BACKGROUND);

        //Buttons
        JPanel actionsBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionsBar.setBackground(BACKGROUND);

        JButton btnAdd = createStyledButton("Add User", ACCENT, Color.WHITE);
        JButton btnEdit = createStyledButton("Edit", ACCENT, Color.WHITE);
        JButton btnDelete = createStyledButton("Delete", ACCENT, Color.WHITE);
        JButton btnRefresh = createStyledButton("Refresh", ACCENT, Color.WHITE);
        JButton btnSort = createStyledButton("Sort A-Z", ACCENT, Color.WHITE);

        btnAdd.addActionListener(e -> handleAddUser());
        btnEdit.addActionListener(e -> handleEditUser());
        btnDelete.addActionListener(e -> handleDeleteUser());
        btnRefresh.addActionListener(e -> loadUsersData());
        btnSort.addActionListener(e -> handleSortUsers());

        actionsBar.add(btnAdd);
        actionsBar.add(btnEdit);
        actionsBar.add(btnDelete);
        actionsBar.add(btnRefresh);
        actionsBar.add(btnSort);

        contentPanel.add(actionsBar, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Username", "Role", "Status", "Last Login"};
        usersModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        usersTable = new JTable(usersModel);
        styleTable(usersTable);

        JScrollPane scrollPane = new JScrollPane(usersTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        scrollPane.getViewport().setBackground(BACKGROUND);

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private void loadUsersData() {
        LoadingDialog.showLoading(this);
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                return UserService.listUsers();
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> rows = get();
                    usersModel.setRowCount(0);
                    if (rows != null) {
                        for (Map<String, Object> r : rows) {
                            usersModel.addRow(new Object[]{
                                r.get("user_id"),
                                r.get("username"),
                                r.get("role"),
                                r.get("status"),
                                r.get("last_login")
                            });
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    // A-Z sorting
    private void handleSortUsers() {
        LoadingDialog.showLoading(this);
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                List<Map<String, Object>> rows = UserService.listUsers();
                rows.sort((r1, r2) -> {
                    String u1 = (String) r1.get("username");
                    String u2 = (String) r2.get("username");
                    if (u1 == null) u1 = "";
                    if (u2 == null) u2 = "";
                    return u1.compareToIgnoreCase(u2);
                });
                return rows;
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> rows = get();
                    usersModel.setRowCount(0);
                    if (rows != null) {
                        for (Map<String, Object> r : rows) {
                            usersModel.addRow(new Object[]{
                                r.get("user_id"),
                                r.get("username"),
                                r.get("role"),
                                r.get("status"),
                                r.get("last_login")
                            });
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    //    ACTION HANDLERS
    private void handleAddCourse() {
        CourseDialog d = new CourseDialog(this, null);
        d.setVisible(true);
        if (!d.isOk()) return;

        LoadingDialog.showLoading(this);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return CourseService.addCourse(d.getCode(), d.getTitle(), d.getCredits());
            }

            @Override
            protected void done() {
                LoadingDialog.hideLoading();
                try {
                    boolean added = get();
                    if (added) {
                        showMessage("Course added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);

                        Map<String, Object> newCourse = CourseService.findByCode(d.getCode());
                        if (newCourse != null) {
                            int courseId = (int) newCourse.get("course_id");
                            offerToCreateDefaultSection(courseId);
                        }
                        loadCoursesData();
                    } else {
                        showMessage("Duplicate course code.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showMessage("Error adding course: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void offerToCreateDefaultSection(int courseId) {
        int response = JOptionPane.showConfirmDialog(this,
            "Course created successfully!\nWould you like to create a default section?",
            "Create Default Section",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            List<Map<String, Object>> instructors = SectionService.listInstructors();

            if (instructors.isEmpty()) {
                SectionService.createSection(courseId, null, "TBA", "TBA", 30, "Fall", 2025);
                showMessage("Default section created without instructor.", 
                          "Info", JOptionPane.INFORMATION_MESSAGE);
            } else {
                InstructorSelectionDialog instrDialog = new InstructorSelectionDialog(this, instructors);
                instrDialog.setVisible(true);

                if (instrDialog.isConfirmed()) {
                    Integer instructorId = instrDialog.getSelectedInstructorId();
                    SectionService.createSection(courseId, instructorId, "TBA", "TBA", 30, "Fall", 2025);
                    showMessage("Default section created successfully!", 
                              "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    private void handleEditCourse() {
        int r = coursesTable.getSelectedRow();
        if (r < 0) {
            showMessage("Please select a course to edit.", "Select Course", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int id = (int) coursesModel.getValueAt(r, 0);
        Map<String, Object> course = CourseService.findById(id);
        if (course == null) {
            showMessage("Course not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        CourseDialog d = new CourseDialog(this, course);
        d.setVisible(true);
        if (!d.isOk()) return;

        LoadingDialog.showLoading(this);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return CourseService.updateCourse(id, d.getCode(), d.getTitle(), d.getCredits());
            }

            @Override
            protected void done() {
                LoadingDialog.hideLoading();
                try {
                    boolean ok = get();
                    if (ok) {
                        showMessage("Course updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadCoursesData();
                    } else {
                        showMessage("Failed to update course.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showMessage("Error updating course: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void handleDeleteCourse() {
        int r = coursesTable.getSelectedRow();
        if (r < 0) {
            showMessage("Please select a course to delete.", "Select Course", 
                        JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int id = (int) coursesModel.getValueAt(r, 0);
        int ans = JOptionPane.showConfirmDialog(this,
            "Delete this course?\nAll associated sections and enrollments will be removed.",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (ans != JOptionPane.YES_OPTION) return;

        if (CourseService.deleteCourse(id)) {
            showMessage("Course deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadCoursesData();
        } else {
            showMessage("Failed to delete course.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //    ACTION HANDLERS - SECTIONS   
    private void handleAddSection() {
        SectionDialog dlg = new SectionDialog(this);
        dlg.setVisible(true);
        if (!dlg.isOk()) return;
        
        boolean ok = SectionService.createSection(
            dlg.getCourseId(),
            dlg.getInstructorId(),
            dlg.getDayTime(),
            dlg.getRoom(),
            dlg.getCapacity(),
            dlg.getSemester().isEmpty() ? "Fall" : dlg.getSemester(),
            dlg.getYear()
        );
        
        if (ok) {
            showMessage("Section created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadSectionsData();
        } else {
            showMessage("Failed to create section.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleEditSection() {
        int row = sectionsTable.getSelectedRow();
        if (row < 0) {
            showMessage("Select a section to edit.", "Select", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int sectionId = (int) sectionsModel.getValueAt(row, 0);
        EditSectionDialog dlg = new EditSectionDialog(this, sectionId);
        dlg.setVisible(true);
        
        if (dlg.isSaved()) {
            loadSectionsData();
        }
    }

    private void handleDeleteSection() {
        int row = sectionsTable.getSelectedRow();
        if (row < 0) {
            showMessage("Select a section to delete.", "Select", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int sectionId = (int) sectionsModel.getValueAt(row, 0);
        int ans = JOptionPane.showConfirmDialog(this, 
            "Delete section ID " + sectionId + "?", 
            "Confirm", JOptionPane.YES_NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;
        
        LoadingDialog.showLoading(this);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return deleteSection(sectionId);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        showMessage("Section deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadSectionsData();
                    } else {
                        showMessage("Failed to delete section.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showMessage("Failed to delete section: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    private boolean deleteSection(int sectionId) {
        String delSection = "DELETE FROM sections WHERE section_id = ?";
        
        try (Connection conn = DBManager.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(delSection)) {
            ps.setInt(1, sectionId);
            int r = ps.executeUpdate();
            return r > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    //    ACTION HANDLERS - USERS   
    private void handleAddUser() {
        RegisterUI.openFromAdmin(this);
        loadUsersData();
    }

    private void handleEditUser() {
        int r = usersTable.getSelectedRow();
        if (r < 0) {
            showMessage("Select a user to edit.", "Select", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Integer userId = (Integer) usersModel.getValueAt(r, 0);
        Map<String, Object> u = UserService.findById(userId);
        if (u == null) {
            showMessage("Selected user not found.", "Error", JOptionPane.ERROR_MESSAGE);
            loadUsersData();
            return;
        }

        UserDialog dlg = new UserDialog(this, u);
        dlg.setVisible(true);
        if (!dlg.isOk()) return;

        boolean ok = UserService.updateUser(userId, dlg.getUsername(), dlg.getRole(), 
                                            dlg.getPassword(), dlg.getStatus());
        if (ok) {
            showMessage("User updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadUsersData();
        } else {
            showMessage("Failed to update user.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteUser() {
        int r = usersTable.getSelectedRow();
        if (r < 0) {
            showMessage("Select a user to delete.", "Select", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Integer userId = (Integer) usersModel.getValueAt(r, 0);
        String username = (String) usersModel.getValueAt(r, 1);
        
        int ans = JOptionPane.showConfirmDialog(this, 
            "Delete user " + username + "?", 
            "Confirm", JOptionPane.YES_NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;

        boolean ok = UserService.deleteUser(userId);
        if (ok) {
            showMessage("User deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadUsersData();
        } else {
            showMessage("Failed to delete user. Check FK constraints.", "Error", 
                        JOptionPane.ERROR_MESSAGE);
        }
    }

    //    OTHER HANDLERS   
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to logout?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
    
    private void handleChangePassword() {
        ChangePasswordDialog dialog = new ChangePasswordDialog(this, user.getUserId());
        dialog.setVisible(true);
    }

    private void toggleMaintenance() {
        final boolean desired = maintenanceCheckbox.isSelected();

        LoadingDialog.showLoading(this);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    settingsDao.set("maintenance_on", desired ? "true" : "false");
                    return true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                boolean ok;
                try {
                    ok = get();
                } catch (Exception e) {
                    ok = false;
                } finally {
                    LoadingDialog.hideLoading();
                }

                if (!ok) {
                    maintenanceCheckbox.setSelected(!desired);
                    showMessage("Failed to change maintenance state. Check logs.",
                              "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                showMessage("Maintenance Mode is now " + (desired ? "ON" : "OFF"),
                          "Maintenance", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    // Interface Design
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
        
        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            Color originalBg = bg;
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.darker());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(originalBg);
            }
        });
        
        return btn;
    }

    private void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(BACKGROUND);
        table.setSelectionBackground(ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setRowHeight(32);
        table.setGridColor(new Color(231, 220, 197, 100));
        table.setShowGrid(true);
        table.setShowVerticalLines(false);
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setForeground(Color.WHITE);
        header.setBackground(PRIMARY);
        header.setBorder(BorderFactory.createLineBorder(ACCENT));
        header.setPreferredSize(new Dimension(0, 70));
        header.setReorderingAllowed(false);
    }
    
    private JScrollPane createTableScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        scrollPane.getViewport().setBackground(BACKGROUND);
        return scrollPane;
    }

    private void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    // Choose Instructor 
    private static class InstructorSelectionDialog extends JDialog {
        private final JComboBox<InstructorItem> cbInstructor = new JComboBox<InstructorItem>();
        private boolean confirmed = false;

        public InstructorSelectionDialog(JFrame parent, List<Map<String, Object>> instructors) {
            super(parent, "Select Instructor for Section", true);

            setLayout(new BorderLayout(10, 10));

            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
            contentPanel.setBackground(BACKGROUND);

            JLabel label = new JLabel("Select an instructor for this section:");
            label.setFont(new Font("SansSerif", Font.PLAIN, 14));
            label.setForeground(TEXT_PRIMARY);

            cbInstructor.addItem(new InstructorItem(null, "-- No Instructor (assign later) --"));

            for (Map<String, Object> instructor : instructors) {
                Integer id = (Integer) instructor.get("user_id");
                String name = (String) instructor.get("username");
                cbInstructor.addItem(new InstructorItem(id, name));
            }

            contentPanel.add(label, BorderLayout.NORTH);
            contentPanel.add(cbInstructor, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(BACKGROUND);
            
            JButton btnConfirm = new JButton("Confirm");
            btnConfirm.setBackground(PRIMARY);
            btnConfirm.setForeground(Color.WHITE);
            btnConfirm.setFocusPainted(false);
            
            JButton btnCancel = new JButton("Cancel");
            btnCancel.setBackground(PANEL);
            btnCancel.setForeground(TEXT_PRIMARY);
            btnCancel.setFocusPainted(false);

            btnConfirm.addActionListener(e -> {
                confirmed = true;
                dispose();
            });

            btnCancel.addActionListener(e -> {
                confirmed = false;
                dispose();
            });

            buttonPanel.add(btnConfirm);
            buttonPanel.add(btnCancel);

            add(contentPanel, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);

            getContentPane().setBackground(BACKGROUND);
            pack();
            setSize(400, 180);
            setLocationRelativeTo(parent);
            setResizable(false);
        }

        public boolean isConfirmed() {
            return confirmed;
        }

        public Integer getSelectedInstructorId() {
            InstructorItem selected = (InstructorItem) cbInstructor.getSelectedItem();
            return selected != null ? selected.id : null;
        }

        private static class InstructorItem {
            final Integer id;
            final String name;

            InstructorItem(Integer id, String name) {
                this.id = id;
                this.name = name;
            }

            @Override
            public String toString() {
                return name;
            }
        }
    }

    // REGISTRATION WINDOW EDITOR   
    private static class EditRegistrationDialog extends JDialog {
        private final JSpinner startSpinner;
        private final JSpinner endSpinner;
        private final JSpinner dropSpinner;  // NEW
        private boolean saved = false;
        private final int sectionId;

        public EditRegistrationDialog(JFrame parent, int sectionId, Timestamp curStart, Timestamp curEnd) {
            super(parent, "Edit Registration & Drop Window - Section " + sectionId, true);
            this.sectionId = sectionId;

            setLayout(new BorderLayout(10,10));
            JPanel content = new JPanel(new GridBagLayout());
            content.setBorder(new EmptyBorder(12,12,12,12));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(6,6,6,6);
            c.fill = GridBagConstraints.HORIZONTAL;

            //  FETCH CURRENT DROP DEADLINE 
            Timestamp[] windows = SectionService.getRegistrationAndDropWindow(sectionId);
            Timestamp curDrop = windows[2];

            startSpinner = new JSpinner(new SpinnerDateModel(
                    curStart != null ? new java.util.Date(curStart.getTime()) : new java.util.Date(), null, null, java.util.Calendar.MINUTE));
            startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "yyyy-MM-dd HH:mm"));

            endSpinner = new JSpinner(new SpinnerDateModel(
                    curEnd != null ? new java.util.Date(curEnd.getTime()) : new java.util.Date(System.currentTimeMillis() + 86400000L), null, null, java.util.Calendar.MINUTE));
            endSpinner.setEditor(new JSpinner.DateEditor(endSpinner, "yyyy-MM-dd HH:mm"));

            dropSpinner = new JSpinner(new SpinnerDateModel(
                    curDrop != null ? new java.util.Date(curDrop.getTime()) : new java.util.Date(System.currentTimeMillis() + 86400000L * 7), null, null, java.util.Calendar.MINUTE));
            dropSpinner.setEditor(new JSpinner.DateEditor(dropSpinner, "yyyy-MM-dd HH:mm"));

            c.gridx = 0; c.gridy = 0;
            content.add(new JLabel("Registration start:"), c);
            c.gridx = 1; content.add(startSpinner, c);
            
            c.gridx = 0; c.gridy = 1;
            content.add(new JLabel("Registration end:"), c);
            c.gridx = 1; content.add(endSpinner, c);
            
            c.gridx = 0; c.gridy = 2;
            content.add(new JLabel("Drop deadline:"), c);
            c.gridx = 1; content.add(dropSpinner, c);

            add(content, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnSave = new JButton("Save");
            JButton btnCancel = new JButton("Cancel");
            buttons.add(btnSave);
            buttons.add(btnCancel);
            add(buttons, BorderLayout.SOUTH);

            btnSave.addActionListener(e -> onSave());
            btnCancel.addActionListener(e -> dispose());

            pack();
            setSize(500, 250);
            setLocationRelativeTo(parent);
        }

        private void onSave() {
            java.util.Date startDate = (java.util.Date) startSpinner.getValue();
            java.util.Date endDate = (java.util.Date) endSpinner.getValue();
            java.util.Date dropDate = (java.util.Date) dropSpinner.getValue();
            
            // Validation
            if (startDate != null && endDate != null && endDate.before(startDate)) {
                JOptionPane.showMessageDialog(this, "Registration end must be after start.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (dropDate != null && endDate != null && dropDate.before(endDate)) {
                JOptionPane.showMessageDialog(this, "Drop deadline should be after registration end.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Timestamp startTs = startDate == null ? null : new Timestamp(startDate.getTime());
            Timestamp endTs   = endDate == null ? null : new Timestamp(endDate.getTime());
            Timestamp dropTs  = dropDate == null ? null : new Timestamp(dropDate.getTime());

            boolean ok = SectionService.updateRegistrationAndDropWindow(sectionId, startTs, endTs, dropTs);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Failed to save. Check logs.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            saved = true;
            dispose();
        }

        public boolean isSaved() { return saved; }
    }

    // BACKUPS PAGE    
    private JPanel createBackupsPanel() {
        JPanel panel = createBasePanel("Database Backups");
        JPanel contentPanel = (JPanel) panel.getComponent(1);

        JPanel actionsBar = createActionBar();
        addButtonToBar(actionsBar, "Create Backup", ACCENT, Color.WHITE, e -> handleCreateBackup());
        addButtonToBar(actionsBar, "Restore Selected", ACCENT, Color.WHITE, e -> handleRestoreBackup());
        addButtonToBar(actionsBar, "Delete Selected", ACCENT, Color.WHITE, e -> handleDeleteBackup());
        addButtonToBar(actionsBar, "Refresh", ACCENT, Color.WHITE, e -> loadBackupsData());
        
        JLabel pathLabel = new JLabel("  Location: " + BACKUP_DIR);
        pathLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        actionsBar.add(pathLabel);
        
        contentPanel.add(actionsBar, BorderLayout.NORTH);

        String[] columns = {"Filename", "Date Created", "Size (KB)"};
        backupsModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };

        backupsTable = new JTable(backupsModel);
        styleTable(backupsTable);
        contentPanel.add(createTableScrollPane(backupsTable), BorderLayout.CENTER);

        return panel;
    }

    private void loadBackupsData() {
        LoadingDialog.showLoading(this);
        SwingWorker<java.io.File[], Void> worker = new SwingWorker<>() {
            @Override
            protected java.io.File[] doInBackground() throws Exception {
                java.io.File dir = new java.io.File(BACKUP_DIR);
                if (!dir.exists()) dir.mkdirs();
                java.io.File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".sql"));
                if (files != null) {
                    java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                }
                return files;
            }

            @Override
            protected void done() {
                try {
                    java.io.File[] files = get();
                    backupsModel.setRowCount(0);
                    if (files != null) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        for (java.io.File f : files) {
                            backupsModel.addRow(new Object[]{
                                f.getName(),
                                sdf.format(new java.util.Date(f.lastModified())),
                                String.format("%.2f KB", f.length() / 1024.0)
                            });
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    private void handleCreateBackup() {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
        String filename = "erp_backup_" + timestamp + ".sql";
        java.io.File saveFile = new java.io.File(BACKUP_DIR, filename);

        LoadingDialog.showLoading(this);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() { return performMysqlDump(saveFile); }
            @Override
            protected void done() {
                try {
                    if (get()) {
                        showMessage("Backup created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadBackupsData();
                    } else {
                        showMessage("Backup failed. Check config.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) { e.printStackTrace(); }
                finally { LoadingDialog.hideLoading(); }
            }
        }.execute();
    }

    private void handleRestoreBackup() {
        int r = backupsTable.getSelectedRow();
        if (r < 0) { showMessage("Select a file to restore.", "Select File", JOptionPane.INFORMATION_MESSAGE); return; }
        
        String filename = (String) backupsModel.getValueAt(r, 0);
        java.io.File file = new java.io.File(BACKUP_DIR, filename);

        int confirm = JOptionPane.showConfirmDialog(this, 
            "WARNING: This will OVERWRITE your database with:\n" + filename + "\nAre you sure?", 
            "Confirm Restore", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
        if (confirm != JOptionPane.YES_OPTION) return;

        LoadingDialog.showLoading(this);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() { return performMysqlRestore(file); }
            @Override
            protected void done() {
                try {
                    if (get()) showMessage("Database restored successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    else showMessage("Restore failed.", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception e) { e.printStackTrace(); }
                finally { LoadingDialog.hideLoading(); }
            }
        }.execute();
    }

    private JPanel createBasePanel(String titleText) {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel title = new JLabel(titleText);
        title.setFont(FONT_TITLE);
        title.setForeground(PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBackground(BACKGROUND);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setBackground(BACKGROUND);
        return bar;
    }

    private void addButtonToBar(JPanel bar, String text, Color bg, Color fg, java.awt.event.ActionListener al) {
        JButton btn = createStyledButton(text, bg, fg);
        btn.addActionListener(al);
        bar.add(btn);
    }

    private void handleDeleteBackup() {
        int r = backupsTable.getSelectedRow();
        if (r < 0) { showMessage("Select a file to delete.", "Select File", JOptionPane.INFORMATION_MESSAGE); return; }
        
        String filename = (String) backupsModel.getValueAt(r, 0);
        if (JOptionPane.showConfirmDialog(this, "Delete " + filename + "?", "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        LoadingDialog.showLoading(this);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return new java.io.File(BACKUP_DIR, filename).delete();
            }

            @Override
            protected void done() {
                try {
                    get();
                    loadBackupsData();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showMessage("Failed to delete file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    private boolean performMysqlDump(java.io.File saveFile) {
        List<String> cmd = new ArrayList<>();
        cmd.add(MYSQL_BIN_PATH + "mysqldump.exe");
        cmd.add("-uroot");
        cmd.add("-p122023!@#");
        cmd.add("--databases");
        cmd.add("univ_erp");
        cmd.add("univ_auth");
        cmd.add("--result-file=" + saveFile.getAbsolutePath());

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            while (reader.readLine() != null) { }
            
            int exitCode = p.waitFor();
            System.out.println("[BACKUP] Exit code: " + exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean performMysqlRestore(java.io.File backupFile) {
        List<String> cmd = new ArrayList<>();
        cmd.add(MYSQL_BIN_PATH + "mysql.exe");
        cmd.add("-uroot");
        cmd.add("-p122023!@#");

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectInput(backupFile);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            while (reader.readLine() != null) { }
            
            int exitCode = p.waitFor();
            System.out.println("[RESTORE] Exit code: " + exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}