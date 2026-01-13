package edu.univ.erp.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import edu.univ.erp.model.AuthUser;
import edu.univ.erp.service.EvaluationService;
import edu.univ.erp.service.GradeService;
import edu.univ.erp.service.StudentService;

public class EnrollmentsDialog extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(EnrollmentsDialog.class.getName());

    private final Integer courseId;
    private final Integer sectionId;
    private final String label;
    private final boolean forGrading;
    private final AuthUser currentUser;
    private DefaultTableModel studentsModel;
    private JTable studentsTable;
    private DefaultTableModel gradesModel;
    private JTable gradesTable;
    private JPanel gradesPanel;

    public EnrollmentsDialog(Frame parent, int sectionId, String sectionLabel, boolean forGrading, AuthUser currentUser) {
        this(parent, null, sectionId, sectionLabel, forGrading, currentUser);
    }

    public EnrollmentsDialog(Frame parent, int courseId, String courseLabel) {
        this(parent, courseId, null, courseLabel, false, null);
    }

    public EnrollmentsDialog(Frame parent, int sectionId, String sectionLabel, boolean forGrading) {
        this(parent, null, sectionId, sectionLabel, forGrading, null);
    }

    private EnrollmentsDialog(Frame parent, Integer courseId, Integer sectionId, String label, boolean forGrading, AuthUser currentUser) {
        super(parent, (forGrading ? "Edit Marks - " : "Enrollments - ") + label, true);
        this.courseId = courseId;
        this.sectionId = sectionId;
        this.label = label;
        this.forGrading = forGrading;
        this.currentUser = currentUser;

        setSize(forGrading ? 1200 : 700, 600);
        setLocationRelativeTo(parent);

        initUI();
        loadStudents();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        if (forGrading) {
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setLeftComponent(createStudentsPanel());
            splitPane.setRightComponent(createGradesPanel());
            splitPane.setDividerLocation(400);
            add(splitPane, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnSave = new JButton("Save Marks");
            JButton btnManageComponents = new JButton("Manage Components");
            JButton btnClose = new JButton("Close");

            btnSave.addActionListener(e -> saveSelectedStudentMarks());
            btnManageComponents.addActionListener(e -> openComponentsDialog());
            btnClose.addActionListener(e -> dispose());

            buttonPanel.add(btnSave);
            buttonPanel.add(btnManageComponents);
            buttonPanel.add(btnClose);
            add(buttonPanel, BorderLayout.SOUTH);
        } else {
            add(createStudentsPanel(), BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnClose = new JButton("Close");
            btnClose.addActionListener(e -> dispose());
            buttonPanel.add(btnClose);
            add(buttonPanel, BorderLayout.SOUTH);
        }
    }

    private JPanel createStudentsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Students"));

        String[] columns = {"Roll No", "Name", "Program", "Enrollment ID"};
        studentsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        studentsTable = new JTable(studentsModel);
        studentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        if (forGrading) {
            studentsTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    loadGradesForSelectedStudent();
                }
            });
        }

        JScrollPane scrollPane = new JScrollPane(studentsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGradesPanel() {
        gradesPanel = new JPanel(new BorderLayout(5, 5));
        gradesPanel.setBorder(BorderFactory.createTitledBorder("Grades"));

        String[] columns = {"Component", "Max Score", "Weight %", "Score", "Final Grade"};
        gradesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (row < 0 || row >= getRowCount()) return false;
                Object val = getValueAt(row, 0);
                String component = val != null ? val.toString() : "";
                if ("FINAL TOTAL".equalsIgnoreCase(component) || "---".equals(component)) {
                    return false;
                }
                return column == 3 || column == 4;
            }
        };

        gradesTable = new JTable(gradesModel);
        gradesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gradesModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() != TableModelEvent.UPDATE) return;
                int row = e.getFirstRow();
                int col = e.getColumn(); 
                if (row < 0 || row >= gradesModel.getRowCount()) return;

                Object compObj = gradesModel.getValueAt(row, 0);
                String component = compObj != null ? compObj.toString() : "";
                if (component == null) return;
                if ("FINAL TOTAL".equalsIgnoreCase(component) || "FINAL LETTER".equalsIgnoreCase(component) || "---".equals(component)) {
                    return;
                }

                if (col == TableModelEvent.ALL_COLUMNS || col == 3 || col == 1 || col == -1) {
                    Object scoreObj = gradesModel.getValueAt(row, 3);
                    Object maxObj = gradesModel.getValueAt(row, 1);

                    if (scoreObj == null || maxObj == null) {
                        gradesModel.setValueAt("", row, 4);
                        return;
                    }

                    String scoreStr = scoreObj.toString().trim();
                    String maxStr = maxObj.toString().trim();
                    if (scoreStr.isEmpty() || maxStr.isEmpty()) {
                        gradesModel.setValueAt("", row, 4);
                        return;
                    }

                    try {
                        BigDecimal score = new BigDecimal(scoreStr);
                        BigDecimal max = new BigDecimal(maxStr);
                        if (max.compareTo(BigDecimal.ZERO) <= 0) {
                            gradesModel.setValueAt("", row, 4);
                            return;
                        }
                        // computing percentage
                        BigDecimal pct = score.divide(max, 6, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                        String letter = computeLetterGrade(pct);
                        gradesModel.setValueAt(letter, row, 4);
                    } catch (NumberFormatException ne) {
                        gradesModel.setValueAt("", row, 4);
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Failed to compute letter grade", ex);
                        gradesModel.setValueAt("", row, 4);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(gradesTable);
        gradesPanel.add(scrollPane, BorderLayout.CENTER);

        return gradesPanel;
    }

    private void loadStudents() {
        studentsModel.setRowCount(0);

        List<Map<String, Object>> students;
        try {
            if (sectionId != null) {
                students = StudentService.listStudentsEnrolledInCourse(sectionId);
            } else if (courseId != null) {
                students = StudentService.listStudentsEnrolledInCourse(courseId);
            } else {
                return;
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Failed to load students for section/course", t);
            JOptionPane.showMessageDialog(this,
                    "Failed to load students: " + t.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (Map<String, Object> student : students) {
            Object nameObj = student.get("username");
            String name = nameObj != null ? nameObj.toString() : "";

            Object programObj = student.get("program");
            String program = programObj != null ? programObj.toString() : "";

            studentsModel.addRow(new Object[]{
                    student.get("roll_no"),
                    name,
                    program,
                    student.get("enrollment_id")
            });
        }
    }

    private void loadGradesForSelectedStudent() {
        if (!forGrading || gradesModel == null) return;

        gradesModel.setRowCount(0);

        int selectedRow = studentsTable.getSelectedRow();
        if (selectedRow < 0) return;

        Object enrollmentObj = studentsModel.getValueAt(selectedRow, 3);
        if (!(enrollmentObj instanceof Number)) {
            LOGGER.warning("Selected enrollment id is not a number: " + enrollmentObj);
            return;
        }
        int enrollmentId = ((Number) enrollmentObj).intValue();

        if (sectionId == null) {
            JOptionPane.showMessageDialog(this, "Section context is missing for grading.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer courseForSection;
        try {
            courseForSection = GradeService.getCourseIdForSection(sectionId);
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Failed to get course for section " + sectionId, t);
            JOptionPane.showMessageDialog(this, "Error getting course: " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (courseForSection == null) return;

        // Loading Components
        List<Map<String, Object>> components;
        try {
            components = GradeService.listEvaluationComponentsForCourse(courseForSection);
            if (components.isEmpty()) {
                EvaluationService.addComponent(sectionId, "Midsem", 25.0, 25.0);
                EvaluationService.addComponent(sectionId, "Endsem", 35.0, 35.0);
                EvaluationService.addComponent(sectionId, "Assignment", 20.0, 10.0);
                EvaluationService.addComponent(sectionId, "Quiz", 20.0, 10.0);
                components = GradeService.listEvaluationComponentsForCourse(courseForSection);
            }
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(this, "Failed to load components: " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Loading Grades
        List<Map<String, Object>> grades;
        try {
            grades = GradeService.listGradesForEnrollment(enrollmentId);
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(this, "Failed to load grades: " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, Map<String, Object>> gradeMap = new java.util.HashMap<>();
        for (Map<String, Object> grade : grades) {
            String component = (String) grade.get("component");
            if (component != null) {
                gradeMap.put(component.toUpperCase(), grade);
            }
        }

        // Displaying Components
        for (Map<String, Object> component : components) {
            String componentName = null;
            if (component.get("component_name") != null) componentName = component.get("component_name").toString();
            else if (component.get("name") != null) componentName = component.get("name").toString();
            else componentName = "UNKNOWN";

            Map<String, Object> gradeData = gradeMap.get(componentName.toUpperCase());

            BigDecimal score = null;
            String finalGrade = null;

            if (gradeData != null) {
                Object scoreObj = gradeData.get("score");
                if (scoreObj instanceof BigDecimal) score = (BigDecimal) scoreObj;
                else if (scoreObj instanceof Number) score = new BigDecimal(((Number) scoreObj).toString());
                else if (scoreObj != null) {
                    try { score = new BigDecimal(scoreObj.toString()); } catch (Exception ex) {}
                }

                Object fg = gradeData.get("final_grade");
                if (fg != null) finalGrade = fg.toString();
            }

            Object maxScore = component.get("max_score");
            Object weightPct = component.get("weight_pct");

            String componentLetterGrade = "";
            if (score != null && maxScore != null) {
                try {
                    double scoreVal = score.doubleValue();
                    double maxScoreVal = ((Number) maxScore).doubleValue();
                    if (maxScoreVal > 0) {
                        double percentage = (scoreVal / maxScoreVal) * 100.0;
                        componentLetterGrade = GradeService.percentageToLetterGrade(percentage);
                    }
                } catch (Exception e) {}
            }

            String displayGrade = (finalGrade != null && !finalGrade.trim().isEmpty()) ? finalGrade : componentLetterGrade;

            gradesModel.addRow(new Object[]{ componentName, maxScore != null ? maxScore : "", weightPct != null ? weightPct : "", score != null ? score : "", displayGrade });
        }

    }

    private void saveSelectedStudentMarks() {
        if (gradesTable.isEditing()) {
            gradesTable.getCellEditor().stopCellEditing();
        }

        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "User context is missing.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int studentRow = studentsTable.getSelectedRow();
        if (studentRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a student first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Object enrollmentObj = studentsModel.getValueAt(studentRow, 3);
        if (!(enrollmentObj instanceof Number)) return;
        int enrollmentId = ((Number) enrollmentObj).intValue();

        BigDecimal calculatedTotal = BigDecimal.ZERO;

        for (int i = 0; i < gradesModel.getRowCount(); i++) {
            String component = String.valueOf(gradesModel.getValueAt(i, 0));

            if ("---".equals(component)) continue;
            if ("FINAL TOTAL".equalsIgnoreCase(component)) continue;

            String dbComponent = component;
            if ("FINAL LETTER".equalsIgnoreCase(component)) {
                dbComponent = "FINAL_LETTER";
            }

            // fetching data 
            Object scoreObj = gradesModel.getValueAt(i, 3);
            Object finalGradeObj = gradesModel.getValueAt(i, 4);
            Object maxObj = gradesModel.getValueAt(i, 1);
            Object weightObj = gradesModel.getValueAt(i, 2);

            BigDecimal score = null;
            if (scoreObj != null) {
                String scoreStr = scoreObj.toString().trim();
                if (!scoreStr.isEmpty()) {
                    try {
                        score = new BigDecimal(scoreStr);
                        if (!"FINAL_LETTER".equals(dbComponent) && maxObj != null && weightObj != null) {
                            BigDecimal max = new BigDecimal(maxObj.toString());
                            BigDecimal weight = new BigDecimal(weightObj.toString());
                            if (max.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal contribution = score.divide(max, 4, RoundingMode.HALF_UP).multiply(weight);
                                calculatedTotal = calculatedTotal.add(contribution);
                            }
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid score for " + component, "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            String finalGrade = (finalGradeObj == null || finalGradeObj.toString().trim().isEmpty()) ? null : finalGradeObj.toString().trim();

            // Saving Grades
            try {
                if (!GradeService.upsertGrade(currentUser.getUserId(), enrollmentId, dbComponent, score, finalGrade)) {
                    JOptionPane.showMessageDialog(this, "Failed to save " + component, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception re) {
                re.printStackTrace();
            }
        }

        // Saving Calculation
        try {
            calculatedTotal = calculatedTotal.setScale(2, RoundingMode.HALF_UP);
            GradeService.upsertGrade(currentUser.getUserId(), enrollmentId, "FINAL_TOTAL", calculatedTotal, null);

            JOptionPane.showMessageDialog(this, "Marks saved. Final Total: " + calculatedTotal, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving total: " + ex.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
        }

        loadGradesForSelectedStudent();
    }

    private void openComponentsDialog() {
        if (sectionId == null || currentUser == null) return;
        ComponentsDialog cd = new ComponentsDialog((Frame) getOwner(), sectionId, label, currentUser);
        cd.setVisible(true);
        if (studentsTable.getSelectedRow() >= 0) loadGradesForSelectedStudent();
    }

    private String computeLetterGrade(BigDecimal percentage) {
        if (percentage == null) return "";
        BigDecimal pct = percentage.setScale(2, RoundingMode.HALF_UP);
        double p = pct.doubleValue();

        if (p >= 95.0) return "A+";
        if (p >= 90.0) return "A";
        if (p >= 85.0) return "A-";
        if (p >= 80.0) return "B+";
        if (p >= 75.0) return "B";
        if (p >= 70.0) return "B-";
        if (p >= 65.0) return "C+";
        if (p >= 60.0) return "C";
        if (p >= 50.0) return "D";
        return "F";
    }
}