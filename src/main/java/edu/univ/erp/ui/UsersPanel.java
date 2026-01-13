package edu.univ.erp.ui;

import edu.univ.erp.service.UserService;
import edu.univ.erp.util.LoadingDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class UsersPanel extends JFrame {
    private final DefaultTableModel model;
    private final JTable table;

    public UsersPanel() {
        setTitle("Manage Users");
        setSize(900, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        String[] cols = {"ID", "Username", "Role", "Status", "Last Login"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("Users"), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAdd = new JButton("Add User");
        JButton btnEdit = new JButton("Edit User");
        JButton btnDelete = new JButton("Delete User");
        JButton btnRefresh = new JButton("Refresh");
        actions.add(btnAdd); actions.add(btnEdit); actions.add(btnDelete); actions.add(btnRefresh);

        btnAdd.addActionListener(e -> onAdd());
        btnEdit.addActionListener(e -> onEdit());
        btnDelete.addActionListener(e -> onDelete());
        btnRefresh.addActionListener(e -> loadUsers());

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);

        loadUsers();
    }

    private void loadUsers() {
        model.setRowCount(0);
        LoadingDialog.showLoading(this);

        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            Exception error = null;

            @Override
            protected List<Map<String, Object>> doInBackground() {
                try {
                    return UserService.listUsers();
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
                                UsersPanel.this,
                                "Failed to load users: " + error.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }

                    List<Map<String, Object>> rows = null;
                    try {
                        rows = get();
                    } catch (InterruptedException | ExecutionException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(
                                UsersPanel.this,
                                "Failed to load users: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }

                    for (Map<String, Object> r : rows) {
                        model.addRow(new Object[]{
                                r.get("user_id"),
                                r.get("username"),
                                r.get("role"),
                                r.get("status"),
                                r.get("last_login")
                        });
                    }
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };

        worker.execute();
    }

    private void onAdd() {
        RegisterUI.openFromAdmin(this);
        loadUsers();
    }


    private void onEdit() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a user to edit.", "Select", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Integer userId = (Integer) model.getValueAt(r, 0);
        Map<String, Object> u = UserService.findById(userId);
        if (u == null) {
            JOptionPane.showMessageDialog(this, "Selected user not found.", "Error", JOptionPane.ERROR_MESSAGE);
            loadUsers();
            return;
        }
        UserDialog dlg = new UserDialog(this, u);
        dlg.setVisible(true);
        if (!dlg.isOk()) return;
        boolean ok = UserService.updateUser(userId, dlg.getUsername(), dlg.getRole(), dlg.getPassword(), dlg.getStatus());
        if (ok) {
            JOptionPane.showMessageDialog(this, "User updated.");
            loadUsers();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update user.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDelete() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a user to delete.", "Select", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Integer userId = (Integer) model.getValueAt(r, 0);
        String username = (String) model.getValueAt(r, 1);
        int ans = JOptionPane.showConfirmDialog(this, "Delete user " + username + " ?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;
        boolean ok = UserService.deleteUser(userId);
        if (ok) {
            JOptionPane.showMessageDialog(this, "User deleted.");
            loadUsers();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to delete user. Check FK constraints.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
