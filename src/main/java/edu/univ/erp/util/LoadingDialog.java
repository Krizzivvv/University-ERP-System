// File: src/main/java/edu/univ/erp/util/LoadingDialog.java
package edu.univ.erp.util;

import javax.swing.*;
import java.awt.*;

public class LoadingDialog extends JDialog {
    private static LoadingDialog instance;
    
    private LoadingDialog(Window owner) {
        super(owner, "Please Wait", ModalityType.MODELESS);
        setUndecorated(true);
        setLayout(new BorderLayout(10, 10));
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(46, 79, 79), 2),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));
        panel.setBackground(new Color(245, 239, 221));
        
        JLabel label = new JLabel("Loading, please wait...");
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(new Color(62, 39, 35));
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(200, 20));
        
        panel.add(label, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        
        add(panel);
        pack();
        setLocationRelativeTo(owner);
    }
    
    // FIXED: Renamed from show() to showLoading()
    public static void showLoading(Window owner) {
        SwingUtilities.invokeLater(() -> {
            if (instance == null || !instance.isDisplayable()) {
                instance = new LoadingDialog(owner);
            }
            instance.setVisible(true);
        });
    }
    
    // FIXED: Renamed from hide() to hideLoading()
    public static void hideLoading() {
        SwingUtilities.invokeLater(() -> {
            if (instance != null) {
                instance.setVisible(false);
                instance.dispose();
                instance = null;
            }
        });
    }
}