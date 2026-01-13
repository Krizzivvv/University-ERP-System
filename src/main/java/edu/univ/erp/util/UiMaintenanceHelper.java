package edu.univ.erp.util;

import javax.swing.*;
import java.awt.*;

public final class UiMaintenanceHelper {
    private UiMaintenanceHelper() {}
    
    /**
     * Create a maintenance banner
     */
    public static JLabel createMaintenanceBanner(String text) {
        JLabel banner = new JLabel(text, JLabel.CENTER);
        banner.setOpaque(true);
        banner.setBackground(Color.YELLOW);
        banner.setForeground(Color.BLACK);
        banner.setVisible(false);
        return banner;
    }
    
    /**
     * Update banner visibility based on maintenance state
     */
    public static void updateBanner(JLabel banner, MaintenanceChecker checker) {
        boolean isOn = checker.isMaintenanceOn();
        banner.setVisible(isOn);
    }
    
    /**
     * Check maintenance and show warning if active
     * @return true if maintenance is ON (should block action)
     */
    public static boolean checkAndWarn(Component parent, MaintenanceChecker checker, String action) {
        if (checker.isMaintenanceOn()) {
            JOptionPane.showMessageDialog(parent, 
                "Maintenance mode is ON. Cannot " + action + ".", 
                "Maintenance Mode", 
                JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }
}