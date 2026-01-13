package edu.univ.erp.ui;

import edu.univ.erp.service.SectionService;
import edu.univ.erp.util.LoadingDialog;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class EditSectionDialog extends JDialog {
    
    private static final Color BACKGROUND = new Color(245, 239, 221);
    private static final Color PRIMARY = new Color(46, 79, 79);
    private static final Color ACCENT = new Color(158, 179, 132);
    private static final Color TEXT_PRIMARY = new Color(62, 39, 35);
    private static final Color ERROR = new Color(165, 42, 42);

    private final JComboBox<SectionInstructor> cbInstructor = new JComboBox<>();
    private final JTextField tfDayTime = new JTextField(20);
    private final JTextField tfRoom = new JTextField(10);
    private final JSpinner spCapacity = new JSpinner(new SpinnerNumberModel(30, 1, 500, 1));
    private final JTextField tfSemester = new JTextField(8);
    private final JSpinner spYear = new JSpinner(new SpinnerNumberModel(2025, 2000, 2100, 1));

    private JButton btnSave;

    private final int sectionId;
    private boolean saved = false;

    public EditSectionDialog(Window owner, int sectionId) {
        super(owner, "Edit Section " + sectionId, ModalityType.APPLICATION_MODAL);
        this.sectionId = sectionId;
        initUI();
        loadData();
    }

    private void initUI() {
        JPanel p = new JPanel(new MigLayout("fill, insets 20", "[right]10[grow]", "[]10[]10[]10[]10[]10[]10[]push[]"));
        p.setBackground(BACKGROUND);
        styleComboBox(cbInstructor);
        styleTextField(tfDayTime);
        styleTextField(tfRoom);
        styleSpinner(spCapacity);
        styleTextField(tfSemester);
        styleSpinner(spYear);

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
        btnSave = createStyledButton("Save Changes", PRIMARY, BACKGROUND);
        JButton btnCancel = createStyledButton("Cancel", ERROR, Color.WHITE);

        JPanel bottom = new JPanel(new MigLayout("insets 0", "push[]10[]", "[]"));
        bottom.setBackground(BACKGROUND);
        bottom.add(btnSave, "width 120!");
        bottom.add(btnCancel, "width 100!");

        p.add(bottom, "span, growx, gaptop 20");

        btnSave.addActionListener(e -> onSave());
        btnCancel.addActionListener(e -> {
            saved = false;
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

    // LOGIC 

    private void loadData() {
        LoadingDialog.showLoading(this);

        SwingWorker<LoadResult, Void> worker = new SwingWorker<>() {
            @Override
            protected LoadResult doInBackground() throws Exception {
                LoadResult res = new LoadResult();
                try {
                    List<Map<String,Object>> ins = SectionService.listInstructors();
                    Map<String,Object> sec = SectionService.findSectionById(sectionId);

                    res.instructors = ins;
                    res.section = sec;
                } catch (Exception ex) {
                    res.error = ex;
                }
                return res;
            }

            @Override
            protected void done() {
                try {
                    LoadResult res = get();
                    if (res.error != null) {
                        res.error.printStackTrace();
                        JOptionPane.showMessageDialog(EditSectionDialog.this, "Error loading section data.", "Error", JOptionPane.ERROR_MESSAGE);
                        saved = false;
                        setVisible(false);
                        return;
                    }
                    cbInstructor.removeAllItems();
                    cbInstructor.addItem(new SectionInstructor(null, "-- unassigned --"));
                    if (res.instructors != null) {
                        for (Map<String,Object> r : res.instructors) {
                            Integer id = (Integer) r.get("user_id");
                            String name = (String) r.get("username");
                            cbInstructor.addItem(new SectionInstructor(id, name));
                        }
                    }

                    Map<String,Object> sec = res.section;
                    if (sec == null) {
                        JOptionPane.showMessageDialog(EditSectionDialog.this, "Section not found.", "Error", JOptionPane.ERROR_MESSAGE);
                        saved = false;
                        setVisible(false);
                        return;
                    }

                    tfDayTime.setText((String) sec.getOrDefault("day_time", ""));
                    tfRoom.setText((String) sec.getOrDefault("room", ""));
                    spCapacity.setValue(sec.getOrDefault("capacity", 30));
                    tfSemester.setText((String) sec.getOrDefault("semester", ""));
                    spYear.setValue(sec.getOrDefault("year", 2025));

                    Object instrObj = sec.get("instructor_id");
                    if (instrObj != null) {
                        Integer instrId = (Integer) instrObj;
                        for (int i = 0; i < cbInstructor.getItemCount(); i++) {
                            SectionInstructor it = cbInstructor.getItemAt(i);
                            if (it.id != null && it.id.equals(instrId)) {
                                cbInstructor.setSelectedIndex(i);
                                break;
                            }
                        }
                    } else {
                        cbInstructor.setSelectedIndex(0);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(EditSectionDialog.this, "Error finishing load.", "Error", JOptionPane.ERROR_MESSAGE);
                    saved = false;
                    setVisible(false);
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };

        worker.execute();
    }

    private void onSave() {
        SectionInstructor sel = (SectionInstructor) cbInstructor.getSelectedItem();
        Integer instrId = sel == null ? null : sel.id;
        String dayTime = tfDayTime.getText().trim();
        String room = tfRoom.getText().trim();
        int capacity = (Integer) spCapacity.getValue();
        String semester = tfSemester.getText().trim();
        int year = (Integer) spYear.getValue();

        btnSave.setEnabled(false);
        LoadingDialog.showLoading(this);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    return SectionService.updateSection(sectionId, instrId, dayTime, room, capacity, semester, year);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        saved = true;
                        JOptionPane.showMessageDialog(EditSectionDialog.this, "Section updated.");
                        setVisible(false);
                    } else {
                        JOptionPane.showMessageDialog(EditSectionDialog.this, "Failed to update section.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(EditSectionDialog.this, "Error while saving section.", "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnSave.setEnabled(true);
                    LoadingDialog.hideLoading();
                }
            }
        };

        worker.execute();
    }

    public boolean isSaved() { return saved; }

    private static class SectionInstructor {
        final Integer id; final String label;
        SectionInstructor(Integer id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    private static class LoadResult {
        List<Map<String,Object>> instructors;
        Map<String,Object> section;
        Exception error;
    }
}