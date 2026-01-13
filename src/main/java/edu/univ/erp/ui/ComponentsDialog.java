package edu.univ.erp.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.SwingWorker;

import edu.univ.erp.model.AuthUser;
import edu.univ.erp.service.EvaluationService;
import edu.univ.erp.util.ParseUtil;
import edu.univ.erp.util.LoadingDialog;

public class ComponentsDialog extends JDialog {
    private final int sectionId;
    private final DefaultTableModel model;
    private final JTable table;
    private final AuthUser currentUser;

    private static final String[] FIXED_COMPONENTS = {"Midsem", "Endsem", "Assignment", "Quiz"};
    private static final double[] DEFAULT_MAX_SCORES = {25.0, 35.0, 10.0, 10.0}; // max scores
    private static final double[] DEFAULT_WEIGHTS = {25.0, 35.0, 20.0, 20.0}; // percentages

    public ComponentsDialog(Frame owner, Integer sectionId, String sectionLabel, AuthUser currentUser) {
        super(owner, "Manage Component Weights - " + sectionLabel, ModalityType.APPLICATION_MODAL);

        this.sectionId = sectionId == null ? 0 : sectionId;
        this.currentUser = currentUser;

        setSize(600, 360);
        setLocationRelativeTo(owner);

        // Table: Component, Weight, Max Score
        model = new DefaultTableModel(new String[]{"Component","Weight (%)","Max Score"}, 0) {
            @Override 
            public boolean isCellEditable(int row, int col) { 
               return col == 1 || col == 2;
            }
        };
        table = new JTable(model);
        
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Save Weights");
        JButton btnClose = new JButton("Close");
        bottom.add(btnSave);
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> onSaveWeights());
        btnClose.addActionListener(e -> setVisible(false));

        loadOrInitializeComponents();
    }

    //Load existing components
    private void loadOrInitializeComponents() {
        LoadingDialog.showLoading(this);
        SwingWorker<LoadResult, Void> worker = new SwingWorker<>() {
            @Override
            protected LoadResult doInBackground() {
                LoadResult res = new LoadResult();
                try {
                    List<Map<String,Object>> rows = EvaluationService.listComponentsForSection(sectionId);
                    boolean needsInit = rows == null || rows.size() != FIXED_COMPONENTS.length;
                    if (needsInit) {
                        for (int i = 0; i < FIXED_COMPONENTS.length; i++) {
                            String name = FIXED_COMPONENTS[i];
                            double weight = DEFAULT_WEIGHTS[i];
                            double maxScore = DEFAULT_MAX_SCORES[i];
                            EvaluationService.addComponent(sectionId, name, weight, maxScore);
                        }
                        rows = EvaluationService.listComponentsForSection(sectionId);
                        res.initialized = true;
                    }
                    res.rows = rows;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    res.error = ex.getMessage();
                }
                return res;
            }

            @Override
            protected void done() {
                try {
                    LoadResult r = get();
                    model.setRowCount(0);
                    if (r.error != null) {
                        JOptionPane.showMessageDialog(ComponentsDialog.this,
                                "Failed to load components: " + r.error,
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (r.rows != null) {
                        for (Map<String,Object> comp : r.rows) {
                            model.addRow(new Object[]{
                                comp.get("name"),
                                comp.get("weight"),
                                comp.get("max_score")
                            });
                        }
                    }
                    if (r.initialized) {
                        JOptionPane.showMessageDialog(ComponentsDialog.this, 
                            "Default components created:\n" +
                            "Midsem (25%, max=25), Endsem (35%, max=35)\n" +
                            "Assignment (20%, max=10), Quiz (20%, max=10)",
                            "Components Initialized", 
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ComponentsDialog.this,
                        "Unexpected error while loading components: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    private static class LoadResult {
        List<Map<String,Object>> rows;
        boolean initialized = false;
        String error = null;
    }

    //Save updated weights
    private void onSaveWeights() {
        // Stop any active editing
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        List<ComponentUpdate> updates = new ArrayList<>();
        double totalWeight = 0.0;
        for (int i = 0; i < model.getRowCount(); i++) {
            String name = String.valueOf(model.getValueAt(i, 0));
            double weight = ParseUtil.toDouble(model.getValueAt(i, 1), 0.0);
            double maxScore = ParseUtil.toDouble(model.getValueAt(i, 2), 0.0);
            updates.add(new ComponentUpdate(name, weight, maxScore));
            totalWeight += weight;
        }
        if (Math.abs(totalWeight - 100.0) > 0.01) {
            JOptionPane.showMessageDialog(this,
                String.format("ERROR: Total weight must equal 100%%!\n\nCurrent total: %.2f%%\n\n" +
                            "Please adjust the weights so they add up to exactly 100%%.", totalWeight),
                "Invalid Weights",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        for (ComponentUpdate cu : updates) {
            if (cu.maxScore <= 0) {
                JOptionPane.showMessageDialog(this,
                    "ERROR: Max score for '" + cu.name + "' must be greater than 0!",
                    "Invalid Max Score",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Save component weights and max scores?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        LoadingDialog.showLoading(this);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            String errorMsg = null;
            @Override
            protected Boolean doInBackground() {
                try {
                    List<Map<String,Object>> existing = EvaluationService.listComponentsForSection(sectionId);
                    Map<String,Integer> nameToId = new HashMap<>();
                    if (existing != null) {
                        for (Map<String,Object> e : existing) {
                            String n = String.valueOf(e.get("name"));
                            Integer id = ParseUtil.toInt(e.get("component_id"));
                            nameToId.put(n, id);
                        }
                    }
                    boolean allSuccess = true;
                    for (ComponentUpdate cu : updates) {
                        Integer compId = nameToId.get(cu.name);
                        if (compId == null) {
                            EvaluationService.addComponent(sectionId, cu.name, cu.weight, cu.maxScore);
                            existing = EvaluationService.listComponentsForSection(sectionId);
                            nameToId.clear();
                            if (existing != null) {
                                for (Map<String,Object> e : existing) {
                                    String n = String.valueOf(e.get("name"));
                                    Integer id = ParseUtil.toInt(e.get("component_id"));
                                    nameToId.put(n, id);
                                }
                            }
                            compId = nameToId.get(cu.name);
                            if (compId == null) {
                                allSuccess = false;
                                continue;
                            }
                        }
                        boolean ok = EvaluationService.updateComponent(compId, cu.name, cu.weight, cu.maxScore);
                        if (!ok) allSuccess = false;
                    }
                    return allSuccess;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    errorMsg = ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        JOptionPane.showMessageDialog(ComponentsDialog.this, 
                            "Component weights and max scores saved successfully!\nTotal: 100%", 
                            "Success", 
                            JOptionPane.INFORMATION_MESSAGE);
                        setVisible(false);
                    } else {
                        String m = (errorMsg != null) ? errorMsg : "Failed to save some components.";
                        JOptionPane.showMessageDialog(ComponentsDialog.this, 
                            m, 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ComponentsDialog.this, 
                        "Unexpected error while saving: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    LoadingDialog.hideLoading();
                }
            }
        };
        worker.execute();
    }

    private static class ComponentUpdate {
        final String name;
        final double weight;
        final double maxScore;
        ComponentUpdate(String name, double weight, double maxScore) {
            this.name = name; this.weight = weight; this.maxScore = maxScore;
        }
    }
}