package edu.univ.erp;

import edu.univ.erp.db.DBManager;
import edu.univ.erp.ui.LoginFrame;
import edu.univ.erp.ui.UIStyle;

import javax.swing.SwingUtilities;

public class AppMain {
    public static void main(String[] args) {
        DBManager.initDatabases();
        UIStyle.initTheme();
        SwingUtilities.invokeLater(() -> {
            LoginFrame.getInstance().setVisible(true);
        });
    }
}