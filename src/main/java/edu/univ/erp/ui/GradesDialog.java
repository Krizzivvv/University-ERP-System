package edu.univ.erp.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.NumberFormatter;

import edu.univ.erp.service.EvaluationService;
import edu.univ.erp.service.GradeService;
import edu.univ.erp.util.LoadingDialog;
import edu.univ.erp.util.ParseUtil;

public class GradesDialog extends JDialog {
    private final int enrollmentId;
    private final int sectionId;
    private final JLabel lblStudent;
    private final DefaultTableModel tableModel;
    private final JTable table;

    public GradesDialog(Window owner, int enrollmentId, int sectionId, String studentLabel) {
        super(owner, "Enter Grades - " + studentLabel, ModalityType.APPLICATION_MODAL);
        this.enrollmentId = enrollmentId;
        this.sectionId = sectionId;

        setLayout(new BorderLayout());
        lblStudent = new JLabel(studentLabel);
        lblStudent.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(lblStudent, BorderLayout.NORTH);
        tableModel = new DefaultTableModel(new String[]{"comp_id", "Component", "Score", "Max", "Weight(%)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return Integer.class;
                    case 2:
                        return Number.class;
                    case 3:
                        return Number.class;
                    case 4:
                        return Number.class;
                    default:
                        return String.class;
                }
            }
        };

        table = new JTable(tableModel);
        table.removeColumn(table.getColumnModel().getColumn(0));
        add(new JScrollPane(table), BorderLayout.CENTER);
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(2);

        NumberFormatter nfFormatter = new NumberFormatter(nf);
        nfFormatter.setValueClass(Double.class);
        nfFormatter.setAllowsInvalid(true);
        nfFormatter.setOverwriteMode(false);

        JFormattedTextField formattedField = new JFormattedTextField(nfFormatter);
        formattedField.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        int scoreViewIndex = table.convertColumnIndexToView(2);
        int maxViewIndex = table.convertColumnIndexToView(3);
        int weightViewIndex = table.convertColumnIndexToView(4);

        table.getColumnModel().getColumn(scoreViewIndex).setCellEditor(new DefaultCellEditor(formattedField));

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(scoreViewIndex).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(maxViewIndex).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(weightViewIndex).setCellRenderer(rightRenderer);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Save & Compute Final");
        JButton btnCancel = new JButton("Cancel");
        bottom.add(btnSave);
        bottom.add(btnCancel);
        add(bottom, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> onSave(btnSave));
        btnCancel.addActionListener(e -> dispose());

        loadComponentsAndValues();

        pack();
        setSize(560, Math.min(420, getHeight()));
        setLocationRelativeTo(owner);
    }

    private void loadComponentsAndValues() {
        tableModel.setRowCount(0);
        List<Map<String,Object>> comps = EvaluationService.listComponentsForSection(sectionId);

        Set<String> processedNames = new HashSet<>();

        try {
            for (Map<String,Object> c : comps) {
                Object secIdObj = c.get("section_id");
                if (secIdObj != null) {
                    try {
                        int cSid = Integer.parseInt(secIdObj.toString());
                        if (cSid != this.sectionId) continue;
                    } catch (Exception ignored) {}
                }

                String name = Objects.toString(c.get("name"), "").trim();
                if (name.isEmpty()) continue;

                if (processedNames.contains(name.toLowerCase())) {
                    continue;
                }
                processedNames.add(name.toLowerCase());

                Object compIdObj = c.get("component_id");
                Integer compId = null;
                if (compIdObj instanceof Number) {
                    compId = ((Number) compIdObj).intValue();
                } else if (compIdObj != null) {
                    try { compId = Integer.parseInt(String.valueOf(compIdObj)); } catch (NumberFormatException ignored) {}
                }

                Number maxScoreNum = ParseUtil.toNumber(c.get("max_score"));
                double maxScore = maxScoreNum == null ? 100.0 : maxScoreNum.doubleValue();
                Number weightNum = ParseUtil.toNumber(c.get("weight"));
                double weight = weightNum == null ? 0.0 : weightNum.doubleValue();
                Double existing = null;
                try {
                    existing = GradeService.getComponentScore(enrollmentId, name);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                double scoreVal = existing == null ? 0.0 : existing;
                tableModel.addRow(new Object[]{ compId, name, scoreVal, maxScore, weight });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load components: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void onSave(JButton btnSave) {
        try {
            if (table.isEditing()) {
                if (!table.getCellEditor().stopCellEditing()) {
                    table.getCellEditor().cancelCellEditing();
                }
            }

            int rows = tableModel.getRowCount();
            Map<String, Number> compMap = new HashMap<>(rows);

            for (int i = 0; i < rows; ++i) {
                Object nameObj = tableModel.getValueAt(i, 1);  
                Object scoreObj = tableModel.getValueAt(i, 2); 

                String compName = Objects.toString(nameObj, "").trim();
                if (compName.isEmpty()) continue;

                Number scoreNum = ParseUtil.toNumber(scoreObj);
                if (scoreNum == null) {
                    scoreNum = Double.valueOf(0.0);
                }

                // START VALIDATION LOGIC

                Object maxObj = tableModel.getValueAt(i, 3);
                double maxScore = ParseUtil.toDouble(maxObj, 0.0);
                double enteredScore = scoreNum.doubleValue();

                if (enteredScore > maxScore) {
                    JOptionPane.showMessageDialog(this,
                        "Error in " + compName + ":\n" +
                        "Entered marks (" + enteredScore + ") cannot exceed Max Score (" + maxScore + ").",
                        "Invalid Marks",
                        JOptionPane.ERROR_MESSAGE);
                    return; 
                }

                if (enteredScore < 0) {
                    JOptionPane.showMessageDialog(this, "Error: Marks cannot be negative.");
                    return;
                }

                compMap.put(compName, scoreNum);
            }

            System.out.println("[GradesDialog] Saving for enrollment=" + enrollmentId + " compMap=" + compMap);

            LoadingDialog.showLoading(GradesDialog.this);
            btnSave.setEnabled(false);

            SwingWorker<SaveResult, Void> worker = new SwingWorker<>() {
                Exception error = null;
                SaveResult result = new SaveResult();

                @Override
                protected SaveResult doInBackground() throws Exception {
                    try {
                        try {
                            GradeService.class.getMethod("saveComponentsAndCompute", int.class, Map.class)
                                    .invoke(null, enrollmentId, compMap);
                        } catch (ReflectiveOperationException ex) {
                            for (Map.Entry<String, Number> e : compMap.entrySet()) {
                                String comp = e.getKey();
                                Number score = e.getValue();
                                try {
                                    GradeService.class.getMethod("setComponentScore", int.class, String.class, double.class)
                                            .invoke(null, enrollmentId, comp, score == null ? 0.0 : score.doubleValue());
                                } catch (ReflectiveOperationException ignored) {
                                } catch (Throwable t) {
                                    if (t.getCause() != null) {
                                        throw new Exception(t.getCause());
                                    } else {
                                        throw new Exception(t);
                                    }
                                }
                            }
                            try {
                                GradeService.class.getMethod("computeAndSaveFinal", int.class).invoke(null, enrollmentId);
                            } catch (ReflectiveOperationException ignored) {}
                        }

                        try {
                            result.finalTotal = GradeService.getComponentScore(enrollmentId, "FINAL_TOTAL");
                        } catch (Exception ignored) {}
                        try {
                            result.finalLetter = GradeService.getFinalLetter(enrollmentId);
                        } catch (Exception ignored) {}

                        result.success = true;
                    } catch (Throwable ex) {
                        error = ex instanceof Exception ? (Exception) ex : new Exception(ex);
                        result.success = false;
                    }
                    return result;
                }

                @Override
                protected void done() {
                    try {
                        SaveResult r = get();
                        if (r.success) {
                            String finalMsg;
                            if (r.finalTotal == null && (r.finalLetter == null || r.finalLetter.trim().isEmpty())) {
                                finalMsg = "Grades saved.";
                            } else {
                                String sTotal = r.finalTotal == null ? "-" : String.format("%.2f", r.finalTotal);
                                String sLetter = r.finalLetter == null ? "-" : r.finalLetter;
                                finalMsg = "Grades saved. Final = " + sTotal + " (" + sLetter + ")";
                            }
                            JOptionPane.showMessageDialog(GradesDialog.this, finalMsg, "OK", JOptionPane.INFORMATION_MESSAGE);
                            dispose();
                        } else {
                            if (error != null) {
                                error.printStackTrace();
                                JOptionPane.showMessageDialog(GradesDialog.this,
                                        "Failed to save grades: " + error.getMessage(),
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(GradesDialog.this,
                                        "Failed to save grades due to unknown error.",
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(GradesDialog.this,
                                "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        LoadingDialog.hideLoading();
                        btnSave.setEnabled(true);
                    }
                }
            };

            worker.execute();


        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private static class SaveResult {
        boolean success = false;
        Double finalTotal;
        String finalLetter;
    }
}
