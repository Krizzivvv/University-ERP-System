package edu.univ.erp.ui;

import edu.univ.erp.service.CourseService;
import edu.univ.erp.service.SectionService;
import edu.univ.erp.util.ComboBoxItem;
import edu.univ.erp.util.LoadingDialog;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class SectionDialog extends JDialog {
    
    private static final Color BACKGROUND = new Color(245, 239, 221);
    private static final Color PRIMARY = new Color(46, 79, 79);
    private static final Color ACCENT = new Color(158, 179, 132);
    private static final Color TEXT_PRIMARY = new Color(62, 39, 35);
    private static final Color ERROR = new Color(165, 42, 42);

    private final JComboBox<ComboBoxItem<Integer>> cbCourse = new JComboBox<>();
    private final JComboBox<ComboBoxItem<Integer>> cbInstructor = new JComboBox<>();
    private final JTextField tfDayTime = new JTextField(20);
    private final JTextField tfRoom = new JTextField(10);
    private final JSpinner spCapacity = new JSpinner(new SpinnerNumberModel(30, 1, 500, 1));
    private final JTextField tfSemester = new JTextField(8);
    private final JSpinner spYear = new JSpinner(new SpinnerNumberModel(2025, 2000, 2100, 1));
    private boolean ok = false;

    public SectionDialog(Window owner) {
        super(owner, "Create Section / Assign Instructor", ModalityType.APPLICATION_MODAL);
        initUI();
        loadData();
    }

    private void initUI() {
        JPanel p = new JPanel(new MigLayout("fill, insets 20", "[right]10[grow]", "[]10[]10[]10[]10[]10[]10[]10[]push[]"));
        p.setBackground(BACKGROUND);

        styleComboBox(cbCourse);
        styleComboBox(cbInstructor);
        styleTextField(tfDayTime);
        styleTextField(tfRoom);
        styleSpinner(spCapacity);
        styleTextField(tfSemester);
        styleSpinner(spYear);

        p.add(createStyledLabel("Course:"));
        p.add(cbCourse, "growx, wrap");

        p.add(createStyledLabel("Instructor:"));
        p.add(cbInstructor, "growx, wrap");

        p.add(createStyledLabel("Day/Time:"));
        p.add(tfDayTime, "growx, wrap");

        p.add(createStyledLabel("Room:"));
        p.add(tfRoom, "growx, wrap");

        p.add(createStyledLabel("Capacity:"));
        p.add(spCapacity, "width 80!, wrap");

        p.add(createStyledLabel("Semester:"));
        p.add(tfSemester, "growx, wrap");

        p.add(createStyledLabel("Year:"));
        p.add(spYear, "width 80!, wrap");

        // Buttons
        JButton btnSave = createStyledButton("Create Section", PRIMARY, BACKGROUND);
        JButton btnCancel = createStyledButton("Cancel", ERROR, Color.WHITE);

        JPanel bottom = new JPanel(new MigLayout("insets 0", "push[]10[]", "[]"));
        bottom.setBackground(BACKGROUND);
        bottom.add(btnSave, "width 120!");
        bottom.add(btnCancel, "width 100!");

        p.add(bottom, "span, growx, gaptop 20");

        btnSave.addActionListener(e -> {
            if (cbCourse.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Select a course.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ok = true;
            setVisible(false);
        });

        btnCancel.addActionListener(e -> {
            ok = false;
            setVisible(false);
        });

        setContentPane(p);
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Sanserif", Font.PLAIN, 13));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(Color.WHITE);
        field.setCaretColor(PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1),
            new EmptyBorder(5, 8, 5, 8)
        ));
    }

    private void styleComboBox(JComboBox<?> box) {
        box.setFont(new Font("Sanserif", Font.PLAIN, 13));
        box.setForeground(TEXT_PRIMARY);
        box.setBackground(Color.WHITE);
        ((JComponent) box.getRenderer()).setBorder(new EmptyBorder(4, 8, 4, 8));
    }

    private void styleSpinner(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField field = ((JSpinner.DefaultEditor) editor).getTextField();
            styleTextField(field);
        }
        spinner.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        spinner.setFont(new Font("Sanserif", Font.PLAIN, 13));
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Sanserif", Font.BOLD, 13));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Sanserif", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            Color originalBg = bg;
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(originalBg);
            }
        });
        
        return btn;
    }

    private void loadData() {
        LoadingDialog.showLoading(this);
        SwingWorker<LoadResult, Void> worker = new SwingWorker<>() {
            Exception error = null;
            LoadResult result = new LoadResult();

            @Override
            protected LoadResult doInBackground() throws Exception {
                try {
                    result.courses = CourseService.listCourses();
                    result.instructors = SectionService.listInstructors();
                } catch (Exception ex) {
                    error = ex;
                }
                return result;
            }

            @Override
            protected void done() {
                try {
                    if (error != null) {
                        error.printStackTrace();
                        JOptionPane.showMessageDialog(SectionDialog.this,
                                "Failed to load data: " + error.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Populate courses
                    cbCourse.removeAllItems();
                    List<Map<String,Object>> courses = result.courses;
                    if (courses != null) {
                        for (Map<String,Object> c : courses) {
                            int id = (Integer) c.get("course_id");
                            String code = (String) c.get("code");
                            String title = (String) c.get("title");
                            cbCourse.addItem(new ComboBoxItem<>(id, code + " - " + title));
                        }
                    }

                    // Populate instructors
                    cbInstructor.removeAllItems();
                    cbInstructor.addItem(new ComboBoxItem<>(null, "-- No Instructor --"));
                    List<Map<String,Object>> instructors = result.instructors;
                    if (instructors != null) {
                        for (Map<String,Object> inst : instructors) {
                            Integer id = (Integer) inst.get("user_id");
                            String name = (String) inst.get("username");
                            String dept = (String) inst.get("department");
                            String label = name + (dept != null ? " (" + dept + ")" : "");
                            cbInstructor.addItem(new ComboBoxItem<>(id, label));
                        }
                    }
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };

        worker.execute();
    }

    private static class LoadResult {
        List<Map<String,Object>> courses;
        List<Map<String,Object>> instructors;
    }

    public boolean isOk() { return ok; }

    public int getCourseId() {
        ComboBoxItem<Integer> it = (ComboBoxItem<Integer>) cbCourse.getSelectedItem();
        return it.getId();
    }

    public Integer getInstructorId() {
        ComboBoxItem<Integer> it = (ComboBoxItem<Integer>) cbInstructor.getSelectedItem();
        return it == null ? null : it.getId();
    }

    public String getDayTime() { return tfDayTime.getText().trim(); }
    public String getRoom() { return tfRoom.getText().trim(); }
    public int getCapacity() { return (Integer) spCapacity.getValue(); }
    public String getSemester() { return tfSemester.getText().trim(); }
    public int getYear() { return (Integer) spYear.getValue(); }
}
