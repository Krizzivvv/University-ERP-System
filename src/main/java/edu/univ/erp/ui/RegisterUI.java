package edu.univ.erp.ui;

import edu.univ.erp.db.DBManager;
import edu.univ.erp.service.AuthService;
import net.miginfocom.swing.MigLayout;
import edu.univ.erp.util.LoadingDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class RegisterUI extends JDialog {
    private static final Color BACKGROUND = new Color(245, 239, 221);
    private static final Color PRIMARY = new Color(46, 79, 79);
    private static final Color ACCENT = new Color(158, 179, 132);
    private static final Color TEXT_PRIMARY = new Color(62, 39, 35);
    private static final Color ERROR = new Color(165, 42, 42);

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<String> comboRole;

    private JTextField txtRollNo;
    private JTextField txtProgram;
    private JTextField txtYear;

    private JButton btnCreate;
    private JButton btnCancel;

    public RegisterUI(Frame owner) {
        super(owner, "Create User", true);
        initUI();
        setLocationRelativeTo(owner);
        pack();
    }

    private void initUI() {
        JPanel main = new JPanel(new MigLayout("fill, insets 20", "[right]10[grow]", "[]10[]10[]10[]10[]10[]push[]"));
        main.setBackground(BACKGROUND);

        txtUsername = new JTextField(20);
        txtPassword = new JPasswordField(20);
        comboRole = new JComboBox<>(new String[]{"student", "instructor", "admin"});
        txtRollNo = new JTextField(12);
        txtProgram = new JTextField(20);
        txtYear = new JTextField(4);

        styleTextField(txtUsername);
        styleTextField(txtPassword); 
        styleComboBox(comboRole);
        styleTextField(txtRollNo);
        styleTextField(txtProgram);
        styleTextField(txtYear);

        // Add Components
        main.add(createStyledLabel("Username:"));
        main.add(txtUsername, "growx, wrap");

        main.add(createStyledLabel("Password:"));
        main.add(txtPassword, "growx, wrap");

        main.add(createStyledLabel("Role:"));
        main.add(comboRole, "growx, wrap");

        // Separator for Student Fields
        JSeparator sep = new JSeparator();
        sep.setForeground(ACCENT);
        main.add(sep, "span, growx, gaptop 10, gapbottom 10, wrap");

        main.add(createStyledLabel("Roll No:"));
        main.add(txtRollNo, "growx, wrap");

        main.add(createStyledLabel("Program:"));
        main.add(txtProgram, "growx, wrap");

        main.add(createStyledLabel("Year:"));
        main.add(txtYear, "width 80!, wrap");

        // Buttons
        btnCreate = createStyledButton("Create", PRIMARY, BACKGROUND);
        btnCancel = createStyledButton("Cancel", ERROR, Color.WHITE);

        JPanel bottom = new JPanel(new MigLayout("insets 0", "push[]10[]", "[]"));
        bottom.setBackground(BACKGROUND);
        bottom.add(btnCreate, "width 100!");
        bottom.add(btnCancel, "width 100!");

        main.add(bottom, "span, growx, gaptop 20");

        setContentPane(main);

        updateStudentFieldsVisibility();

        comboRole.addActionListener(e -> updateStudentFieldsVisibility());
        btnCancel.addActionListener(e -> dispose());
        btnCreate.addActionListener(e -> onCreateUser());
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

    // Logic
    private void updateStudentFieldsVisibility() {
        boolean isStudent = "student".equalsIgnoreCase((String) comboRole.getSelectedItem());
        txtRollNo.setEnabled(isStudent);
        txtProgram.setEnabled(isStudent);
        txtYear.setEnabled(isStudent);
        
        Color bg = isStudent ? Color.WHITE : new Color(240, 240, 240);
        txtRollNo.setBackground(bg);
        txtProgram.setBackground(bg);
        txtYear.setBackground(bg);
    }

    private void onCreateUser() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String role = ((String) comboRole.getSelectedItem()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password are required", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        btnCreate.setEnabled(false);
        LoadingDialog.showLoading(this);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            String message = null;
            Exception error = null;

            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    if ("student".equalsIgnoreCase(role)) {
                        String rollNo = txtRollNo.getText().trim();
                        int year = 1;
                        try {
                            if (!txtYear.getText().isBlank()) year = Integer.parseInt(txtYear.getText().trim());
                        } catch (NumberFormatException ex) {
                            message = "Invalid year value";
                            return false;
                        }

                        String program = txtProgram.getText().trim();
                        if (program.isEmpty()) {
                            message = "Program is required for students";
                            return false;
                        }

                        AuthService authService = new AuthService();
                        boolean ok = authService.registerStudent(username, "student", password, rollNo, program, year);
                        if (!ok) {
                            message = "Failed to create student. Check console for details";
                        }
                        return ok;
                    } else {
                        String insertAuth = "INSERT INTO users_auth (username, role, password_hash, status) VALUES (?, ?, ?, 'active')";
                        String hashed = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt(10));

                        try (Connection conn = DBManager.getAuthConnection();
                             PreparedStatement ps = conn.prepareStatement(insertAuth)) {
                            ps.setString(1, username);
                            ps.setString(2, role);
                            ps.setString(3, hashed);
                            int r = ps.executeUpdate();
                            if (r == 1) {
                                return true;
                            } else {
                                message = "Failed to create user";
                                return false;
                            }
                        } catch (SQLException ex) {
                            error = ex;
                            return false;
                        }
                    }
                } catch (Exception ex) {
                    error = ex;
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = false;
                    try {
                        success = get();
                    } catch (InterruptedException | ExecutionException e) {
                        error = e;
                    }

                    if (success) {
                        JOptionPane.showMessageDialog(RegisterUI.this, "User created successfully");
                        dispose();
                    } else {
                        if (error != null) {
                            error.printStackTrace();
                            JOptionPane.showMessageDialog(RegisterUI.this, "Failed to create user: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        } else if (message != null) {
                            JOptionPane.showMessageDialog(RegisterUI.this, message, "Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(RegisterUI.this, "Failed to create user", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } finally {
                    LoadingDialog.hideLoading();
                    btnCreate.setEnabled(true);
                }
            }
        };

        worker.execute();
    }
    public static void openFromAdmin(Frame owner) {
        RegisterUI dlg = new RegisterUI(owner);
        dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dlg.setVisible(true);
    }
}
