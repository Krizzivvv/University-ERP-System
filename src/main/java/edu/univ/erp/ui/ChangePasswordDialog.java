package edu.univ.erp.ui;

import edu.univ.erp.db.DBManager;
import edu.univ.erp.util.LoadingDialog;
import net.miginfocom.swing.MigLayout;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class ChangePasswordDialog extends JDialog {

    private static final Color BACKGROUND = new Color(245, 239, 221);      // Light sepia
    private static final Color PANEL = new Color(231, 220, 197);           // Deeper sepia
    private static final Color PRIMARY = new Color(46, 79, 79);            // Wine green
    private static final Color ACCENT = new Color(158, 179, 132);          // Sage green
    private static final Color TEXT_PRIMARY = new Color(62, 39, 35);       // Coffee brown

    private final int userId;
    private final JPasswordField pfCurrentPassword = new JPasswordField(20);
    private final JPasswordField pfNewPassword = new JPasswordField(20);
    private final JPasswordField pfConfirmPassword = new JPasswordField(20);
    private boolean changed = false;

    private String backgroundErrorMsg = null;
    private boolean backgroundIncorrectCurrent = false;

    public ChangePasswordDialog(Window owner, int userId) {
        super(owner, "Change Password", ModalityType.APPLICATION_MODAL);
        this.userId = userId;
        initUI();
    }

    private void initUI() {
        getContentPane().setBackground(BACKGROUND);
        setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));

        JPanel card = new JPanel();
        card.setBackground(PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 2),
            new EmptyBorder(20, 30, 20, 30)
        ));
        card.setLayout(new MigLayout("wrap 2, fillx, insets 10", "[][grow, fill]", "[]15[]15[]15[]20[]"));

        JLabel lblHeader = new JLabel("Update Credentials");
        lblHeader.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblHeader.setForeground(PRIMARY);
        card.add(lblHeader, "span 2, center, gapbottom 15");

        card.add(createLabel("Current Password:"));
        styleTextField(pfCurrentPassword);
        card.add(pfCurrentPassword);

        card.add(createLabel("New Password:"));
        styleTextField(pfNewPassword);
        card.add(pfNewPassword);

        card.add(createLabel("Confirm Password:"));
        styleTextField(pfConfirmPassword);
        card.add(pfConfirmPassword);

        JLabel hint = new JLabel("Password must be at least 6 characters");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(new Color(100, 100, 100)); 
        card.add(hint, "skip 1, wrap"); 

        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[]10[]", "[]"));
        buttonPanel.setOpaque(false); 

        JButton btnChange = new JButton("Change Password");
        styleButton(btnChange);
        btnChange.addActionListener(e -> onChangePassword());

        JButton btnCancel = new JButton("Cancel");
        styleLinkButton(btnCancel); 
        btnCancel.addActionListener(e -> dispose());

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnChange);

        card.add(buttonPanel, "span 2, align right");

        add(card);
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    // LOGIC METHODS
    private void onChangePassword() {
        String currentPass = new String(pfCurrentPassword.getPassword());
        String newPass = new String(pfNewPassword.getPassword());
        String confirmPass = new String(pfConfirmPassword.getPassword());

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Validation", JOptionPane.WARNING_MESSAGE);
            pfNewPassword.setText("");
            pfConfirmPassword.setText("");
            return;
        }

        if (newPass.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        backgroundErrorMsg = null;
        backgroundIncorrectCurrent = false;

        LoadingDialog.showLoading(this);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    boolean ok = verifyCurrentPassword(currentPass);
                    if (!ok) {
                        backgroundIncorrectCurrent = true;
                        return false;
                    }
                    boolean updated = updatePassword(newPass);
                    if (!updated) {
                        backgroundErrorMsg = "Could not update password (no rows affected).";
                        return false;
                    }
                    return true;
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    backgroundErrorMsg = ex.getMessage();
                    return false;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    backgroundErrorMsg = ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                LoadingDialog.hideLoading();
                try {
                    boolean result = get();
                    if (result) {
                        JOptionPane.showMessageDialog(ChangePasswordDialog.this, "Password changed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        changed = true;
                        dispose();
                    } else {
                        if (backgroundIncorrectCurrent) {
                            JOptionPane.showMessageDialog(ChangePasswordDialog.this, "Current password is incorrect.", "Error", JOptionPane.ERROR_MESSAGE);
                            pfCurrentPassword.setText("");
                        } else {
                            String msg = (backgroundErrorMsg != null) ? backgroundErrorMsg : "Failed to change password.";
                            JOptionPane.showMessageDialog(ChangePasswordDialog.this, "Failed to change password: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ChangePasswordDialog.this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private boolean verifyCurrentPassword(String currentPass) throws SQLException {
        String sql = "SELECT password_hash FROM users_auth WHERE user_id = ?";
        try (Connection conn = DBManager.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    if (hash != null && hash.startsWith("$2")) {
                        return BCrypt.checkpw(currentPass, hash);
                    } else {
                        return currentPass.equals(hash);
                    }
                }
            }
        }
        return false;
    }

    private boolean updatePassword(String newPass) throws SQLException {
        String newHash = BCrypt.hashpw(newPass, BCrypt.gensalt(12));
        String sql = "UPDATE users_auth SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DBManager.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean isChanged() {
        return changed;
    }

    // LOCAL STYLING HELPERS
    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(BACKGROUND);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1),
            new EmptyBorder(5, 10, 5, 10)
        ));
    }

    private void styleButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(BACKGROUND);
        btn.setBackground(PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                btn.setBackground(PRIMARY.darker());
            }
            public void mouseExited(MouseEvent evt) {
                btn.setBackground(PRIMARY);
            }
        });
    }

    private void styleLinkButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(ACCENT);
        btn.setBackground(PANEL);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                btn.setForeground(PRIMARY);
            }
            public void mouseExited(MouseEvent evt) {
                btn.setForeground(ACCENT);
            }
        });
    }
}