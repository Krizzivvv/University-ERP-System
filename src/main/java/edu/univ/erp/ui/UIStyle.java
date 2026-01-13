package edu.univ.erp.ui;

import javax.swing.*;
import java.awt.*;

public final class UIStyle {
    private UIStyle() {}

    public static void initTheme() {
        UIManager.put("Label.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("Button.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("Table.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("TableHeader.font", new Font("SansSerif", Font.BOLD, 13));
        UIManager.put("TextField.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("ComboBox.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("Button.margin", new Insets(6,12,6,12));
    }

    public static JLabel header(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif Semibold", Font.PLAIN, 18));
        lbl.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        return lbl;
    }

    public static JPanel paddedPanel(int top, int left, int bottom, int right) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(top,left,bottom,right));
        return p;
    }

    public static JPanel actionsPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        return p;
    }
}
