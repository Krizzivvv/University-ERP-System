package edu.univ.erp.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeSlot {
    public final DayOfWeek day;
    public final LocalTime start;
    public final LocalTime end;
    
    public TimeSlot(DayOfWeek day, LocalTime start, LocalTime end) {
        this.day = day;
        this.start = start;
        this.end = end;
    }
    
    public String toDisplayString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        return day.toString() + " " + start.format(fmt) + " - " + end.format(fmt);
    }
    
    @Override
    public String toString() {
        return toDisplayString();
    }
}