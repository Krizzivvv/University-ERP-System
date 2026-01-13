package edu.univ.erp.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;
import net.miginfocom.swing.MigLayout; // Assuming MigLayout is available as per the Dashboard code

public class CourseDialog extends JDialog {
    
    private static final Color BACKGROUND = new Color(245, 239, 221);
    private static final Color PRIMARY = new Color(46, 79, 79);
    private static final Color ACCENT = new Color(158, 179, 132);
    private static final Color TEXT_PRIMARY = new Color(62, 39, 35);
    private static final Color ERROR = new Color(165, 42, 42);

    private final JTextField tfCode = new JTextField(12);
    private final JTextField tfTitle = new JTextField(30);
    private final JSpinner spCredits = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
    private boolean ok = false;
    private Integer courseId = null;

    public CourseDialog(Window owner, Map<String, Object> course) {
        super(owner, "Course", ModalityType.APPLICATION_MODAL);
        initUI(course);
    }

    private void initUI(Map<String, Object> course) {
        JPanel p = new JPanel(new MigLayout("fill, insets 20", "[right]10[grow]", "[]15[]15[]push[]"));
        p.setBackground(BACKGROUND);
        
        styleTextField(tfCode);
        styleTextField(tfTitle);
        styleSpinner(spCredits);

        JLabel lblCode = createStyledLabel("Code:");
        JLabel lblTitle = createStyledLabel("Title:");
        JLabel lblCredits = createStyledLabel("Credits:");
        p.add(lblCode);
        p.add(tfCode, "growx, wrap");
        
        p.add(lblTitle);
        p.add(tfTitle, "growx, wrap");
        
        p.add(lblCredits);
        p.add(spCredits, "width 80!, wrap");

        
        JButton btnOk = createStyledButton("Save", PRIMARY, BACKGROUND);
        JButton btnCancel = createStyledButton("Cancel", ERROR, Color.WHITE);
        
        JPanel bottom = new JPanel(new MigLayout("insets 0", "push[]10[]", "[]"));
        bottom.setBackground(BACKGROUND);
        bottom.add(btnOk, "width 100!");
        bottom.add(btnCancel, "width 100!");
        
        p.add(bottom, "span, growx, gaptop 20");

        btnOk.addActionListener(e -> {
            if (tfCode.getText().trim().isEmpty() || tfTitle.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Code and title required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ok = true;
            this.setVisible(false);
        });

        btnCancel.addActionListener(e -> {
            ok = false;
            this.setVisible(false);
        });

        if (course != null) {
            this.courseId = (Integer) course.get("course_id");
            tfCode.setText((String) course.get("code"));
            tfTitle.setText((String) course.get("title"));
            spCredits.setValue((Integer) course.get("credits"));
        }

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

    public boolean isOk() { return ok; }
    public String getCode() { return tfCode.getText().trim(); }
    public String getTitle() { return tfTitle.getText().trim(); }
    public int getCredits() { return (Integer) spCredits.getValue(); }
    public Integer getCourseId() { return courseId; }
}