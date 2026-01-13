package edu.univ.erp.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class UserDialog extends JDialog {
    private final JTextField tfUsername = new JTextField(20);
    private final JComboBox<String> cbRole = new JComboBox<>(new String[]{"admin","instructor","student"});
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"active","disabled"});
    private final JPasswordField pfPassword = new JPasswordField(20);
    private boolean ok = false;

    public UserDialog(Window owner, Map<String, Object> user) {
        super(owner, "User", ModalityType.APPLICATION_MODAL);
        initUI(user);
    }

    private void initUI(Map<String, Object> u) {
        JPanel p = new JPanel(new BorderLayout(8,8));
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.LINE_END;
        fields.add(new JLabel("Username:"), gbc);
        gbc.gridy++; fields.add(new JLabel("Role:"), gbc);
        gbc.gridy++; fields.add(new JLabel("Status:"), gbc);
        gbc.gridy++; fields.add(new JLabel("Password (opt):"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.LINE_START;
        fields.add(tfUsername, gbc);
        gbc.gridy++; fields.add(cbRole, gbc);
        gbc.gridy++; fields.add(cbStatus, gbc);
        gbc.gridy++; fields.add(pfPassword, gbc);

        p.add(fields, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOk = new JButton("Save");
        JButton btnCancel = new JButton("Cancel");
        bottom.add(btnOk); bottom.add(btnCancel);
        p.add(bottom, BorderLayout.SOUTH);

        btnOk.addActionListener(e -> {
            if (tfUsername.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ok = true;
            setVisible(false);
        });

        btnCancel.addActionListener(e -> {
            ok = false;
            setVisible(false);
        });

        if (u != null) {
            tfUsername.setText((String) u.get("username"));
            cbRole.setSelectedItem((String) u.get("role"));
            cbStatus.setSelectedItem((String) u.get("status"));
        }

        setContentPane(p);
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    public boolean isOk() { return ok; }
    public String getUsername() { return tfUsername.getText().trim(); }
    public String getRole() { return (String) cbRole.getSelectedItem(); }
    public String getStatus() { return (String) cbStatus.getSelectedItem(); }
    public String getPassword() { return new String(pfPassword.getPassword()).trim(); }
}
