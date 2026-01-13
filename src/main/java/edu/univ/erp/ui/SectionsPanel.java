package edu.univ.erp.ui;

import edu.univ.erp.service.SectionService;
import edu.univ.erp.util.LoadingDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import edu.univ.erp.model.TimeSlot;
import java.awt.event.ItemEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class SectionsPanel extends JFrame {
    private final DefaultTableModel model;
    private final JTable table;
    private final int courseId;
    private final String courseLabel;

    private JTextField semesterField;
    private JComboBox<String> semesterCombo;
    private JToggleButton semesterToggle;
    private TimeSlot selectedSlot;
    private JTextField timetableField;

    public SectionsPanel(int courseId, String courseLabel) {
        this.courseId = courseId;
        this.courseLabel = courseLabel;

        setTitle("Sections for " + courseLabel);
        setSize(900, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // creating table model and placing it in center
        String[] cols = {"Section ID", "Instructor", "Day/Time", "Room", "Enrolled/Cap", "Semester", "Year"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);

        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom action bar
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRefresh = new JButton("Refresh");
        JButton btnEdit = new JButton("Edit Section");
        bottom.add(btnEdit);
        bottom.add(btnRefresh);
        add(bottom, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadSections());
        btnEdit.addActionListener(e -> onEdit());

        initComponents();

        loadSections();
    }

    private void loadSections() {
        model.setRowCount(0);
        LoadingDialog.showLoading(this);

        SwingWorker<List<Map<String,Object>>, Void> worker = new SwingWorker<>() {
            Exception error = null;
            List<Map<String,Object>> rows = null;

            @Override
            protected List<Map<String, Object>> doInBackground() {
                try {
                    return SectionService.listSectionsForCourse(courseId);
                } catch (Exception ex) {
                    error = ex;
                    return java.util.Collections.emptyList();
                }
            }

            @Override
            protected void done() {
                try {
                    if (error != null) {
                        JOptionPane.showMessageDialog(
                                SectionsPanel.this,
                                "Failed to load sections: " + error.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    rows = get();
                    for (Map<String,Object> r : rows) {
                        Object instructor = r.get("instructor_username");
                        if (instructor == null) instructor = "-- unassigned --";
                        int enrolled = r.get("enrolled_count") == null ? 0 : (Integer) r.get("enrolled_count");

                        model.addRow(new Object[]{
                                r.get("section_id"),
                                instructor,
                                r.get("day_time"),
                                r.get("room"),
                                enrolled + "/" + r.get("capacity"),
                                r.get("semester"),
                                r.get("year")
                        });
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

    private void openTimetableDialog() {
        TimetableDialog dlg = new TimetableDialog(SwingUtilities.getWindowAncestor(this));
        dlg.setVisible(true);

        if (dlg.isConfirmed()) {
            selectedSlot = dlg.getResult();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            String text = selectedSlot.day.toString()
                    + " " + selectedSlot.start.format(fmt)
                    + " - " + selectedSlot.end.format(fmt);
            timetableField.setText(text);
            JOptionPane.showMessageDialog(this, "Selected: " + text);
        }
    }

    private void onEdit() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a section to edit.");
            return;
        }
        int sectionId = (Integer) model.getValueAt(r, 0);
        EditSectionDialog dlg = new EditSectionDialog(this, sectionId);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            loadSections();
        }
    }

    private void initComponents() {
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        timetableField = new JTextField(20);
        timetableField.setEditable(false);
        timetableField.setText("No timetable set");

        JButton setTimetableButton = new JButton("Set Timetable");
        setTimetableButton.addActionListener(e -> openTimetableDialog());

        gbc.gridx = 0; gbc.gridy = 0;
        top.add(new JLabel("Timetable:"), gbc);

        gbc.gridx = 1;
        top.add(timetableField, gbc);

        gbc.gridx = 2;
        top.add(setTimetableButton, gbc);

        semesterField = new JTextField(8);
        String[] sems = {"1","2","3","4","5","6","7","8"};
        semesterCombo = new JComboBox<>(sems);
        semesterCombo.setVisible(false);

        semesterToggle = new JToggleButton("Use 1..8");
        semesterToggle.addItemListener(e -> {
            boolean useCombo = (e.getStateChange() == ItemEvent.SELECTED);
            semesterCombo.setVisible(useCombo);
            semesterField.setVisible(!useCombo);
            if (useCombo) {
                String t = semesterField.getText().trim();
                for (int i = 0; i < sems.length; i++) if (sems[i].equals(t)) semesterCombo.setSelectedIndex(i);
            } else {
                semesterField.setText((String) semesterCombo.getSelectedItem());
            }
            top.revalidate();
            top.repaint();
        });

        gbc.gridx = 0; gbc.gridy = 1;
        top.add(new JLabel("Semester:"), gbc);

        gbc.gridx = 1;
        top.add(semesterField, gbc);

        gbc.gridx = 2;
        top.add(semesterCombo, gbc);

        gbc.gridx = 3;
        top.add(semesterToggle, gbc);

        add(top, BorderLayout.NORTH);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
            new SectionsPanel(1, "Test Course").setVisible(true)
        );
    }
}
