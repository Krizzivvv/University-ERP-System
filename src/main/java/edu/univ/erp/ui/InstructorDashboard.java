package edu.univ.erp.ui;

import edu.univ.erp.model.AuthUser;
import edu.univ.erp.service.GradeService;
import edu.univ.erp.service.SectionService;
import edu.univ.erp.util.MaintenanceChecker;
import edu.univ.erp.util.ParseUtil;
import edu.univ.erp.util.UiMaintenanceHelper;
import edu.univ.erp.util.LoadingDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;


public class InstructorDashboard extends JFrame {
    private static final Color BACKGROUND = new Color(245, 239, 221);
    private static final Color PANEL = new Color(231, 220, 197);
    private static final Color PRIMARY = new Color(46, 79, 79);
    private static final Color ACCENT = new Color(158, 179, 132);
    private static final Color TEXT_PRIMARY = new Color(62, 39, 35);
    private static final Color TEXT_SECONDARY = new Color(93, 64, 55);
    private static final Color ERROR = new Color(165, 42, 42);
    private static final Color HOVER = new Color(198, 209, 172);

    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 24);
    private static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 20);
    private static final Font FONT_SUB = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONT_BUTTON = new Font("SansSerif", Font.BOLD, 13);
    private static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);

    private static final int SIDEBAR_WIDTH = 220;

    private final AuthUser user;
    private DefaultTableModel sectionTableModel;
    private JTable sectionsTable;
    private JLabel maintenanceBanner;
    private final MaintenanceChecker maintenanceChecker;

    private String currentView = "sections";
    private JPanel centerContentPanel;
    private JButton selectedNavButton = null;

    public InstructorDashboard(AuthUser user) {
        this.user = user;
        this.maintenanceChecker = new MaintenanceChecker();

        setTitle("ERP Portal - Instructor");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);

        initializeUI();
    }

    private void initializeUI() {
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout(0, 0));

        add(createTopHeaderBar(), BorderLayout.NORTH);
        add(createLeftNavigationPanel(), BorderLayout.WEST);
        add(createCenterContentArea(), BorderLayout.CENTER);

        loadSectionsView();

        SwingUtilities.invokeLater(() -> {
            if (sectionsTable != null && sectionsTable.getRowCount() > 0) {
                sectionsTable.requestFocusInWindow();
            }
        });
    }

    //  TOP HEADER BAR 
    private JPanel createTopHeaderBar() {
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

        JLabel roleLabel = new JLabel("Instructor");
        roleLabel.setFont(FONT_SUB);
        roleLabel.setForeground(ACCENT);
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        welcomePanel.add(welcomeLabel);
        welcomePanel.add(Box.createVerticalStrut(4));
        welcomePanel.add(roleLabel);

        JButton logoutButton = createActionButton("Logout", ERROR, Color.WHITE);
        logoutButton.setPreferredSize(new Dimension(110, 38));
        logoutButton.addActionListener(e -> handleLogout());

        headerPanel.add(welcomePanel, BorderLayout.WEST);
        headerPanel.add(logoutButton, BorderLayout.EAST);

        return headerPanel;
    }

    //  LEFT NAVIGATION PANEL 
    private JPanel createLeftNavigationPanel() {
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
        navPanel.add(Box.createVerticalStrut(6));

        JButton btnSections = createNavButton("Course Sections", "sections");
        JButton btnGrades = createNavButton("Manage Grades", "grades");
        JButton btnTranscript = createNavButton("Download Transcript", "transcript");
        JButton btnChangePassword = createNavButton("Change Password", "password");
        JButton btnRefresh = createNavButton("Refresh Data", "refresh");

        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(btnSections);
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(btnGrades);
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(btnTranscript);
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(btnChangePassword);
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(btnRefresh);

        navPanel.add(Box.createVerticalGlue());

        if (maintenanceChecker.isMaintenanceOn()) {
            JLabel maintLabel = new JLabel("MAINTENANCE ON");
            maintLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            maintLabel.setForeground(ERROR);
            maintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            navPanel.add(maintLabel);
        }

        selectedNavButton = btnSections;
        styleNavSelected(btnSections);

        return navPanel;
    }

    private JButton createNavButton(String text, String action) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 14));
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(PANEL);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(SIDEBAR_WIDTH - 24, 42));
        button.setPreferredSize(new Dimension(SIDEBAR_WIDTH - 24, 42));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

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

        button.addActionListener(e -> handleNavigation(action, button));
        return button;
    }

    private void styleNavSelected(JButton btn) {
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
    }

    private void styleNavDeselected(JButton btn) {
        btn.setBackground(PANEL);
        btn.setForeground(TEXT_PRIMARY);
    }

    private void handleNavigation(String action, JButton clickedButton) {
        switch (action) {
            case "sections":
                loadSectionsView();
                break;
            case "grades":
                loadGradesView();
                break;
            case "transcript":
                loadTranscriptView();
                break;
            case "password":
                new ChangePasswordDialog(this, user.getUserId()).setVisible(true);
                return;
            case "refresh":
                refreshCurrentView();
                return;
        }

        if (selectedNavButton != null) styleNavDeselected(selectedNavButton);
        selectedNavButton = clickedButton;
        styleNavSelected(selectedNavButton);
    }

    private JPanel createCenterContentArea() {
        centerContentPanel = new JPanel(new BorderLayout());
        centerContentPanel.setBackground(BACKGROUND);
        centerContentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return centerContentPanel;
    }

    //  SECTIONS VIEW 
    private void loadSectionsView() {
        currentView = "sections";
        centerContentPanel.removeAll();

        if (maintenanceChecker.isMaintenanceOn()) {
            maintenanceBanner = UiMaintenanceHelper.createMaintenanceBanner("MAINTENANCE MODE IS ACTIVE -- View Only");
            centerContentPanel.add(maintenanceBanner, BorderLayout.NORTH);
        }

        JPanel contentPanel = new JPanel(new BorderLayout(0, 14));
        contentPanel.setBackground(BACKGROUND);

        JPanel titlePanel = createTitlePanel("My Course Sections", "Double-click any section to open enrollments");
        contentPanel.add(titlePanel, BorderLayout.NORTH);

        JPanel tablePanel = createSectionsTablePanel();
        contentPanel.add(tablePanel, BorderLayout.CENTER);

        JPanel actionPanel = createSectionsActionPanel();
        contentPanel.add(actionPanel, BorderLayout.SOUTH);

        centerContentPanel.add(contentPanel, BorderLayout.CENTER);
        centerContentPanel.revalidate();
        centerContentPanel.repaint();

        loadSectionsData(); 
    }

    private JPanel createSectionsTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        String[] columns = {"Section ID", "Course (Section)", "Day/Time", "Room", "Enrolled/Cap"};
        sectionTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        sectionsTable = new JTable(sectionTableModel);
        sectionsTable.setRowHeight(34);
        sectionsTable.setFont(FONT_BODY);
        sectionsTable.setSelectionBackground(ACCENT);
        sectionsTable.setSelectionForeground(BACKGROUND);
        sectionsTable.setGridColor(new Color(231, 220, 197, 120));
        sectionsTable.setShowGrid(true);
        sectionsTable.setShowVerticalLines(false);
        sectionsTable.setBackground(BACKGROUND);
        sectionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = sectionsTable.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(BACKGROUND);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 40));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        sectionsTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        sectionsTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        sectionsTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        sectionsTable.getColumnModel().getColumn(1).setPreferredWidth(360);
        sectionsTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        sectionsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        sectionsTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        sectionsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (UiMaintenanceHelper.checkAndWarn(InstructorDashboard.this, maintenanceChecker, "edit marks")) {
                        return;
                    }
                    openSelectedSectionForGradingFromTable();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(sectionsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BACKGROUND);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSectionsActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        panel.setBackground(BACKGROUND);

        JButton btnOpen = createActionButton("Open Section", PRIMARY, Color.WHITE);
        JButton btnViewStats = createActionButton("View Statistics", ACCENT, TEXT_PRIMARY);

        btnOpen.addActionListener(e -> handleOpenSection());
        btnViewStats.addActionListener(e -> handleViewStats());

        panel.add(btnViewStats);
        panel.add(btnOpen);

        return panel;
    }

    /**
     * loadSectionsData now runs in a SwingWorker so it doesn't freeze the UI.
     * Method signature unchanged.
     */
    private void loadSectionsData() {
        sectionTableModel.setRowCount(0);

        LoadingDialog.showLoading(this);
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            Exception error = null;
            List<Map<String, Object>> rows = null;

            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                try {
                    rows = SectionService.listSectionsForInstructor(user.getUserId());
                    return rows;
                } catch (Exception ex) {
                    error = ex;
                    return java.util.Collections.emptyList();
                }
            }

            @Override
            protected void done() {
                try {
                    if (error != null) {
                        error.printStackTrace();
                        sectionTableModel.addRow(new Object[]{"-", "Failed to load sections.", "-", "-", "-"});
                        JOptionPane.showMessageDialog(InstructorDashboard.this,
                                "Failed to load sections: " + error.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    List<Map<String, Object>> result = rows == null ? java.util.Collections.emptyList() : rows;
                    if (result.isEmpty()) {
                        sectionTableModel.addRow(new Object[]{"-", "No sections assigned.", "-", "-", "-"});
                        return;
                    }

                    Map<String, Integer> courseCounters = new java.util.HashMap<>();
                    for (Map<String, Object> r : result) {
                        String courseCode = r.get("course_code") == null ? "" : r.get("course_code").toString();
                        String courseTitle = r.get("course_title") == null ? "" : r.get("course_title").toString();

                        int idx = courseCounters.getOrDefault(courseCode, 0);
                        String sectionLabel = indexToLetters(idx);
                        courseCounters.put(courseCode, idx + 1);

                        String enrolled = r.get("enrolled") == null ? "0" : r.get("enrolled").toString();
                        String cap = r.get("capacity") == null ? "0" : r.get("capacity").toString();

                        sectionTableModel.addRow(new Object[]{
                                r.get("section_id"),
                                courseCode + " - " + courseTitle + " (Sec " + sectionLabel + ")",
                                r.get("day_time"),
                                r.get("room"),
                                enrolled + "/" + cap
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(InstructorDashboard.this,
                            "Failed to finish loading sections: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    //  GRADES VIEW 
    private void loadGradesView() {
        currentView = "grades";
        centerContentPanel.removeAll();

        JPanel contentPanel = new JPanel(new BorderLayout(0, 14));
        contentPanel.setBackground(BACKGROUND);

        JPanel titlePanel = createTitlePanel("Manage Grades", "Select a section to input or edit marks");
        contentPanel.add(titlePanel, BorderLayout.NORTH);

        JPanel selectionPanel = new JPanel(new BorderLayout(10, 10));
        selectionPanel.setBackground(PANEL);
        selectionPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel selectLabel = new JLabel("Select Section:");
        selectLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        selectLabel.setForeground(TEXT_PRIMARY);

        JComboBox<SectionItem> sectionCombo = new JComboBox<>();
        sectionCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sectionCombo.setBackground(Color.WHITE);
        sectionCombo.setForeground(TEXT_PRIMARY);

        LoadingDialog.showLoading(this);
        SwingWorker<List<Map<String,Object>>, Void> comboWorker = new SwingWorker<>() {
            Exception error = null;
            List<Map<String,Object>> sections = null;
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                try {
                    sections = SectionService.listSectionsForInstructor(user.getUserId());
                    return sections;
                } catch (Exception ex) {
                    error = ex;
                    return java.util.Collections.emptyList();
                }
            }

            @Override
            protected void done() {
                try {
                    if (error != null) {
                        error.printStackTrace();
                        JOptionPane.showMessageDialog(InstructorDashboard.this,
                                "Failed to load sections: " + error.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    List<Map<String,Object>> result = sections == null ? java.util.Collections.emptyList() : sections;
                    for (Map<String,Object> sec : result) {
                        int sectionId = ParseUtil.toInt(sec.get("section_id"));
                        String label = sec.get("course_code") + " - " + sec.get("course_title") + " (" + sec.get("day_time") + ")";
                        sectionCombo.addItem(new SectionItem(sectionId, label));
                    }
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        comboWorker.execute();

        JButton btnEditMarks = createActionButton("Edit Marks", PRIMARY, Color.WHITE);
        btnEditMarks.addActionListener(e -> {
            SectionItem selected = (SectionItem) sectionCombo.getSelectedItem();
            if (selected != null) {
                openGradingForSection(selected.id, selected.label);
            }
        });

        JButton btnManageComponents = createActionButton("Manage Components", PRIMARY, Color.WHITE);
        btnManageComponents.addActionListener(e -> {
            if (maintenanceChecker.isMaintenanceOn()) { showMaintenanceWarning("manage components"); return; }
            SectionItem selected = (SectionItem) sectionCombo.getSelectedItem();
            if (selected != null) {
                new ComponentsDialog(this, selected.id, selected.label, user).setVisible(true);
            }
        });

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        inputPanel.setBackground(PANEL);
        inputPanel.add(selectLabel);
        inputPanel.add(sectionCombo);
        inputPanel.add(btnEditMarks);
        inputPanel.add(btnManageComponents);

        selectionPanel.add(inputPanel, BorderLayout.CENTER);
        contentPanel.add(selectionPanel, BorderLayout.CENTER);

        centerContentPanel.add(contentPanel, BorderLayout.CENTER);
        centerContentPanel.revalidate();
        centerContentPanel.repaint();
    }

    //  TRANSCRIPT VIEW 
    private void loadTranscriptView() {
        currentView = "transcript";
        centerContentPanel.removeAll();

        JPanel contentPanel = new JPanel(new BorderLayout(0, 14));
        contentPanel.setBackground(BACKGROUND);

        JPanel titlePanel = createTitlePanel("Export Grade Reports", "Select a section and export grades as CSV");
        contentPanel.add(titlePanel, BorderLayout.NORTH);

        JPanel selectionPanel = new JPanel(new BorderLayout(10, 10));
        selectionPanel.setBackground(PANEL);
        selectionPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel selectLabel = new JLabel("Select Section:");
        selectLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        selectLabel.setForeground(TEXT_PRIMARY);

        JComboBox<SectionItem> sectionCombo = new JComboBox<>();
        sectionCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sectionCombo.setBackground(Color.WHITE);
        sectionCombo.setForeground(TEXT_PRIMARY);

        LoadingDialog.showLoading(this);
        SwingWorker<List<Map<String,Object>>, Void> transComboWorker = new SwingWorker<>() {
            Exception error = null;
            List<Map<String,Object>> sections = null;
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                try {
                    sections = SectionService.listSectionsForInstructor(user.getUserId());
                    return sections;
                } catch (Exception ex) {
                    error = ex;
                    return java.util.Collections.emptyList();
                }
            }

            @Override
            protected void done() {
                try {
                    if (error != null) {
                        error.printStackTrace();
                        JOptionPane.showMessageDialog(InstructorDashboard.this,
                                "Failed to load sections: " + error.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    List<Map<String,Object>> result = sections == null ? java.util.Collections.emptyList() : sections;
                    for (Map<String,Object> sec : result) {
                        int sectionId = ParseUtil.toInt(sec.get("section_id"));
                        String label = sec.get("course_code") + " - " + sec.get("course_title") + " (" + sec.get("day_time") + ")";
                        sectionCombo.addItem(new SectionItem(sectionId, label));
                    }
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        transComboWorker.execute();

        JButton btnExport = createActionButton("Export to CSV", PRIMARY, Color.WHITE);
        btnExport.addActionListener(e -> {
            SectionItem selected = (SectionItem) sectionCombo.getSelectedItem();
            if (selected != null) {
                exportSectionGrades(selected.id, selected.label);
            }
        });

        JButton btnImport = createActionButton("Import from CSV", ACCENT, TEXT_PRIMARY);
        btnImport.addActionListener(e -> {
            SectionItem selected = (SectionItem) sectionCombo.getSelectedItem();
            if (selected != null) {
                importSectionGrades(selected.id, selected.label);
            }
        });

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        inputPanel.setBackground(PANEL);
        inputPanel.add(selectLabel);
        inputPanel.add(sectionCombo);
        inputPanel.add(btnExport);
        inputPanel.add(btnImport);

        selectionPanel.add(inputPanel, BorderLayout.CENTER);
        contentPanel.add(selectionPanel, BorderLayout.CENTER);
        centerContentPanel.add(contentPanel, BorderLayout.CENTER);
        centerContentPanel.revalidate();
        centerContentPanel.repaint();
    }

    private void importSectionGrades(int sectionId, String sectionLabel) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import Grade Report");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File inputFile = fc.getSelectedFile();
        if (inputFile == null) {
            return;
        }

        LoadingDialog.showLoading(this);
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(inputFile))) {
                    String header = reader.readLine();
                    if (header == null || !header.trim().equals("Student ID,Username,Roll No,Midsem,Endsem,Assignment,Quiz,Final Total,Final Grade")) {
                        throw new Exception("Invalid CSV header. Expected: Student ID,Username,Roll No,Midsem,Endsem,Assignment,Quiz,Final Total,Final Grade");
                    }

                    List<Map<String, Object>> enrollments = SectionService.listEnrollmentsForSection(sectionId);
                    Map<Integer, Integer> userToEnrollmentMap = new java.util.HashMap<>();
                    for (Map<String, Object> enrollment : enrollments) {
                        userToEnrollmentMap.put(ParseUtil.toInt(enrollment.get("user_id")), ParseUtil.toInt(enrollment.get("enrollment_id")));
                    }

                    String line;
                    int count = 0;
                    int lineNumber = 1;
                    while ((line = reader.readLine()) != null) {
                        lineNumber++;
                        String[] parts = line.split(",");
                        if (parts.length < 7) continue;

                        int userId = Integer.parseInt(parts[0]);
                        Integer enrollmentId = userToEnrollmentMap.get(userId);
                        if (enrollmentId == null) {
                            throw new Exception("Invalid user ID " + userId + " on line " + lineNumber + ": not enrolled in this section.");
                        }

                        Map<String, Number> compMap = new java.util.HashMap<>();
                        compMap.put("Midsem", Double.parseDouble(parts[3]));
                        compMap.put("Endsem", Double.parseDouble(parts[4]));
                        compMap.put("Assignment", Double.parseDouble(parts[5]));
                        compMap.put("Quiz", Double.parseDouble(parts[6]));

                        GradeService.saveComponentsAndCompute(user.getUserId(), enrollmentId, compMap);
                        count++;
                    }
                    return "Successfully imported grades for " + count + " students.";
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return "Error importing grades: " + ex.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    if (result.startsWith("Error")) {
                        showMessage(result, "Import Failed", JOptionPane.ERROR_MESSAGE);
                    } else {
                        showMessage(result, "Import Complete", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showMessage("Failed to import grades: " + ex.getMessage(), "Import Failed", JOptionPane.ERROR_MESSAGE);
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }


    //  CSV EXPORT
    private void exportSectionGrades(int sectionId, String sectionLabel) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Grade Report");
        String filename = "grades_section_" + sectionId + ".csv";
        fc.setSelectedFile(new File(filename));

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File outputFile = fc.getSelectedFile();

        LoadingDialog.showLoading(this);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            Exception error = null;

            @Override
            protected Void doInBackground() throws Exception {
                List<Map<String,Object>> enrollments = SectionService.listEnrollmentsForSection(sectionId);

                try (FileWriter writer = new FileWriter(outputFile)) {
                    writer.write("Student ID,Username,Roll No,Midsem,Endsem,Assignment,Quiz,Final Total,Final Grade\n");
                    for (Map<String,Object> enrollment : enrollments) {
                        int enrollmentId = ParseUtil.toInt(enrollment.get("enrollment_id"));
                        GradeService.computeAndSaveFinal(user.getUserId(), enrollmentId);
                        int userId = ParseUtil.toInt(enrollment.get("user_id"));
                        String username = String.valueOf(enrollment.get("username"));
                        String rollNo = String.valueOf(enrollment.get("roll_no"));

                        Double midsem = GradeService.getComponentScore(enrollmentId, "Midsem");
                        Double endsem = GradeService.getComponentScore(enrollmentId, "Endsem");
                        Double assignment = GradeService.getComponentScore(enrollmentId, "Assignment");
                        Double quiz = GradeService.getComponentScore(enrollmentId, "Quiz");
                        Double finalTotal = GradeService.getComponentScore(enrollmentId, "FINAL_TOTAL");
                        String finalGrade = GradeService.getFinalLetter(enrollmentId);

                        writer.write(String.format("%d,%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%s\n",
                                userId,
                                username,
                                rollNo,
                                midsem != null ? midsem : 0.0,
                                endsem != null ? endsem : 0.0,
                                assignment != null ? assignment : 0.0,
                                quiz != null ? quiz : 0.0,
                                finalTotal != null ? finalTotal : 0.0,
                                finalGrade != null ? finalGrade : "N/A"
                        ));
                    }
                } catch (Exception ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    if (error != null) {
                        error.printStackTrace();
                        showMessage("Failed to export grades: " + error.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
                    } else {
                        showMessage("Grade report exported successfully to:\n" + outputFile.getAbsolutePath(),
                                "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    }
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };

        worker.execute();
    }

    private static class SectionItem {
        final int id;
        final String label;
        SectionItem(int id, String label) {
            this.id = id;
            this.label = label;
        }
        @Override
        public String toString() { return label; }
    }

    //  ACTION HANDLERS 
    private void handleOpenSection() {
        if (maintenanceChecker.isMaintenanceOn()) {
            showMaintenanceWarning("open section");
            return;
        }
        int row = sectionsTable.getSelectedRow();
        if (row < 0) { showSelectionWarning(); return; }
        int sectionId = ParseUtil.toInt(sectionTableModel.getValueAt(row, 0));
        String secLabel = String.valueOf(sectionTableModel.getValueAt(row, 1));
        new EnrollmentsDialog(this, sectionId, secLabel, false, user).setVisible(true);
    }

    private void handleViewStats() {
        int row = sectionsTable.getSelectedRow();
        if (row < 0) { showSelectionWarning(); return; }
        int sectionId = ParseUtil.toInt(sectionTableModel.getValueAt(row, 0));
        String secLabel = String.valueOf(sectionTableModel.getValueAt(row, 1));

        LoadingDialog.showLoading(this);
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            Exception error = null;
            Map<String, Object> stats = null;

            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                try {
                    stats = GradeService.getClassStats(sectionId);
                    return stats;
                } catch (Exception ex) {
                    error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    if (error != null) {
                        error.printStackTrace();
                        JOptionPane.showMessageDialog(InstructorDashboard.this,
                                "Could not load statistics: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (stats != null) {
                        String message = String.format("Section: %s\n\nClass Size: %s\nAverage Final Grade: %s",
                                secLabel, stats.get("class_size"), stats.get("avg_final_grade"));
                        JOptionPane.showMessageDialog(InstructorDashboard.this, message, "Class Statistics", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(InstructorDashboard.this, "Could not load statistics.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    private void openSelectedSectionForGradingFromTable() {
        int row = sectionsTable.getSelectedRow();
        if (row < 0) { showSelectionWarning(); return; }
        int sectionId = ParseUtil.toInt(sectionTableModel.getValueAt(row, 0));
        String secLabel = String.valueOf(sectionTableModel.getValueAt(row, 1));
        openGradingForSection(sectionId, secLabel);
    }

    private void openGradingForSection(int sectionId, String secLabel) {
        if (maintenanceChecker.isMaintenanceOn()) { showMaintenanceWarning("edit marks"); return; }
        new EnrollmentsDialog(this, sectionId, secLabel, true, user).setVisible(true);
        if (currentView.equals("sections")) {
            loadSectionsData();
        }
    }

    private void handleLogout() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Confirm Logout", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private void refreshCurrentView() {
        switch (currentView) {
            case "sections": loadSectionsView(); break;
            case "grades": loadGradesView(); break;
            case "transcript": loadTranscriptView(); break;
        }
    }

    //  UTILITIES 
    private JPanel createTitlePanel(String title, String subtitle) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(FONT_SUB);
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(subtitleLabel);
        return panel;
    }

    private JButton createActionButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(FONT_BUTTON);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(160, 36));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        button.addMouseListener(new MouseAdapter() {
            Color original = button.getBackground();
            public void mouseEntered(MouseEvent e) { button.setBackground(original.darker()); }
            public void mouseExited(MouseEvent e) { button.setBackground(original); }
        });
        return button;
    }

    private void showMaintenanceWarning(String action) {
        JOptionPane.showMessageDialog(this, "Maintenance mode is active. Cannot " + action + " at this time.", "Maintenance Mode", JOptionPane.WARNING_MESSAGE);
    }

    private void showSelectionWarning() {
        JOptionPane.showMessageDialog(this, "Please select a section first.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String indexToLetters(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index + 1;
        while (n > 0) {
            int rem = (n - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return sb.toString();
    }

    private void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }
}