package edu.univ.erp.ui;

import javax.swing.*;
import java.awt.*;
import edu.univ.erp.model.TimeSlot;
import com.github.lgooddatepicker.components.TimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import java.time.DayOfWeek;
import java.time.LocalTime;

public class TimetableDialog extends JDialog {
    private JComboBox<DayOfWeek> dayCombo;
    private TimePicker startPicker;
    private TimePicker endPicker;
    private JButton okBtn;
    private JButton cancelBtn;

    private boolean confirmed = false;
    private TimeSlot result;

    public TimetableDialog(Window owner) {
        super(owner, "Choose Timetable", ModalityType.APPLICATION_MODAL);
        init();
        pack();
        setLocationRelativeTo(owner);
    }

    private void init() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.anchor = GridBagConstraints.WEST;

        DayOfWeek[] days = new DayOfWeek[]{
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        };
        dayCombo = new JComboBox<>(days);

        // TimePicker settings
        TimePickerSettings tsettings = new TimePickerSettings();
        tsettings.use24HourClockFormat(); 
        tsettings.initialTime = LocalTime.of(9, 0); 
        startPicker = new TimePicker(tsettings);

        TimePickerSettings tsettings2 = new TimePickerSettings();
        tsettings2.use24HourClockFormat();
        tsettings2.initialTime = LocalTime.of(10, 0);
        endPicker = new TimePicker(tsettings2);

        // Add controls
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Day:"), gbc);
        gbc.gridx = 1; add(dayCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Start Time:"), gbc);
        gbc.gridx = 1; add(startPicker, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("End Time:"), gbc);
        gbc.gridx = 1; add(endPicker, gbc);

        // Buttons
        okBtn = new JButton("OK");
        cancelBtn = new JButton("Cancel");
        JPanel bp = new JPanel();
        bp.add(okBtn);
        bp.add(cancelBtn);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        add(bp, gbc);

        // Actions
        okBtn.addActionListener(e -> onOk());
        cancelBtn.addActionListener(e -> { confirmed = false; setVisible(false); });
    }

    private void onOk() {
        DayOfWeek day = (DayOfWeek) dayCombo.getSelectedItem();
        LocalTime start = startPicker.getTime();
        LocalTime end = endPicker.getTime();

        if (start == null || end == null) {
            JOptionPane.showMessageDialog(this, "Please pick both start and end times.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!end.isAfter(start)) {
            JOptionPane.showMessageDialog(this, "End time must be after start time.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        result = new TimeSlot(day, start, end);
        confirmed = true;
        setVisible(false);
    }

    public boolean isConfirmed() { return confirmed; }
    public TimeSlot getResult() { return result; }
}
