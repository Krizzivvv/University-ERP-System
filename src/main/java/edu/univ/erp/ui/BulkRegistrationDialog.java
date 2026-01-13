package edu.univ.erp.ui;

import edu.univ.erp.db.DBManager;
import edu.univ.erp.service.CourseService;
import edu.univ.erp.util.LoadingDialog;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Date;

public class BulkRegistrationDialog extends JDialog {

    private JRadioButton rbAll, rbSelected;
    private JList<CourseItem> courseList;
    private DefaultListModel<CourseItem> listModel = new DefaultListModel<>();
    private JSpinner startSp, endSp, dropSp;
    private boolean applied = false;

    private static final Color BG = new Color(245, 239, 221), PANEL_BG = new Color(231, 220, 197);
    private static final Color PRIMARY = new Color(46, 79, 79), ACCENT = new Color(158, 179, 132);

    private String updateErrorMsg = null;

    public BulkRegistrationDialog(Window owner) {
        super(owner, "Set Registration Windows - Bulk Update", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(BG);
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));

        // Course Selection
        JPanel topPanel = createTitledPanel("Select Courses");
        rbAll = new JRadioButton("Apply to ALL courses");
        rbSelected = new JRadioButton("Apply to selected courses", true);
        styleComps(BG, PRIMARY, rbAll, rbSelected);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbAll); bg.add(rbSelected);

        courseList = new JList<>(listModel);
        courseList.setVisibleRowCount(6);
        courseList.setBackground(BG);
        courseList.setForeground(PRIMARY);

        rbAll.addActionListener(e -> courseList.setEnabled(false));
        rbSelected.addActionListener(e -> courseList.setEnabled(true));

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        radioPanel.setBackground(PANEL_BG);
        radioPanel.add(rbAll); radioPanel.add(rbSelected);

        topPanel.add(radioPanel, BorderLayout.NORTH);
        topPanel.add(new JScrollPane(courseList), BorderLayout.CENTER);

        // 2Date Selection
        JPanel datePanel = createTitledPanel("Registration Window");
        JPanel grid = new JPanel(new GridLayout(3, 2, 10, 10));
        grid.setBackground(PANEL_BG);

        startSp = createDateSpinner(0);
        endSp = createDateSpinner(14); // +2 weeks
        dropSp = createDateSpinner(14); // +3 weeks

        addLabeledRow(grid, "Registration Start:", startSp);
        addLabeledRow(grid, "Registration End:", endSp);
        addLabeledRow(grid, "Drop Deadline:", dropSp);
        datePanel.add(grid, BorderLayout.CENTER);

        JLabel hint = new JLabel("Note: Drop deadline should be after registration end");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(new Color(93, 64, 55));
        datePanel.add(hint, BorderLayout.SOUTH);

        //Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(BG);
        JButton btnApply = createBtn("Apply to Sections", new Color(34, 139, 34), Color.WHITE);
        JButton btnCancel = createBtn("Cancel", PANEL_BG, PRIMARY);

        btnApply.addActionListener(e -> onApply());
        btnCancel.addActionListener(e -> dispose());
        btnPanel.add(btnCancel); btnPanel.add(btnApply);

        add(topPanel, BorderLayout.NORTH);
        add(datePanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        loadCourses();
        pack();
        setSize(600, 550);
        setLocationRelativeTo(owner);
    }

    // run course loading off EDT
    private void loadCourses() {
        LoadingDialog.showLoading(this);
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                return CourseService.listCourses();
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> courses = get();
                    listModel.clear();
                    if (courses != null) {
                        for (Map<String, Object> c : courses) {
                            listModel.addElement(new CourseItem((int)c.get("course_id"), c.get("code") + " - " + c.get("title")));
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(BulkRegistrationDialog.this, "Failed to load courses: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    private void onApply() {
        Date start = (Date) startSp.getValue();
        Date end = (Date) endSp.getValue();
        Date drop = (Date) dropSp.getValue();

        if (end.before(start)) {
            JOptionPane.showMessageDialog(this, "End date must be after start date.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Integer> ids = rbAll.isSelected() ?
            Collections.list(listModel.elements()).stream().map(c -> c.id).collect(Collectors.toList()) :
            courseList.getSelectedValuesList().stream().map(c -> c.id).collect(Collectors.toList());

        if (ids.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one course.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            String.format("Update %d courses?\nStart: %s\nEnd: %s", ids.size(), start, end),
            "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        LoadingDialog.showLoading(this);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                updateErrorMsg = null;
                return updateDB(ids, start, end, drop);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        JOptionPane.showMessageDialog(BulkRegistrationDialog.this, "Successfully updated sections!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        applied = true;
                        dispose();
                    } else {
                        String msg = (updateErrorMsg != null) ? updateErrorMsg : "No rows updated or unknown error.";
                        JOptionPane.showMessageDialog(BulkRegistrationDialog.this, "Update failed: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(BulkRegistrationDialog.this, "Update failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    // Efficient Batch Update
    private boolean updateDB(List<Integer> ids, Date start, Date end, Date drop) {
        String sql = "UPDATE sections SET registration_start=?, registration_end=?, drop_deadline=? WHERE course_id=?";
        try (Connection c = DBManager.getErpConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            c.setAutoCommit(false); // Start transaction
            for (Integer id : ids) {
                ps.setTimestamp(1, new Timestamp(start.getTime()));
                ps.setTimestamp(2, new Timestamp(end.getTime()));
                ps.setTimestamp(3, new Timestamp(drop.getTime()));
                ps.setInt(4, id);
                ps.addBatch();
            }
            int[] rows = ps.executeBatch();
            c.commit();
            return rows.length > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            updateErrorMsg = e.getMessage();
            return false;
        }
    }

    //Helpers
    private JPanel createTitledPanel(String title) {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(PANEL_BG);
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(ACCENT), title,
            TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), PRIMARY));
        return p;
    }

    private JSpinner createDateSpinner(int daysFromNow) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, daysFromNow);
        JSpinner s = new JSpinner(new SpinnerDateModel(cal.getTime(), null, null, Calendar.MINUTE));
        s.setEditor(new JSpinner.DateEditor(s, "yyyy-MM-dd HH:mm"));
        return s;
    }

    private void addLabeledRow(JPanel p, String text, JComponent c) {
        JLabel l = new JLabel(text);
        l.setForeground(PRIMARY);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        p.add(l); p.add(c);
    }

    private JButton createBtn(String txt, Color bg, Color fg) {
        JButton b = new JButton(txt);
        b.setBackground(bg); b.setForeground(fg);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        return b;
    }

    private void styleComps(Color bg, Color fg, JComponent... comps) {
        for(JComponent c : comps) { c.setBackground(bg); c.setForeground(fg); }
    }

    public boolean isApplied() { return applied; }

    private static class CourseItem {
        final int id; final String label;
        CourseItem(int id, String label) { this.id = id; this.label = label; }
        public String toString() { return label; }
    }
}