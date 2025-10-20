package com.scheduler.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Schedule {
    private Map<Day, Map<Shift, List<String>>> assignments;

    public Schedule() {
        this.assignments = new HashMap<>();
        for (Day day : Day.values()) {
            Map<Shift, List<String>> daySchedule = new HashMap<>();
            for (Shift shift : Shift.values()) {
                daySchedule.put(shift, new ArrayList<>());
            }
            assignments.put(day, daySchedule);
        }
    }

    public Map<Day, Map<Shift, List<String>>> getAssignments() {
        return assignments;
    }

    public void setAssignments(Map<Day, Map<Shift, List<String>>> assignments) {
        this.assignments = assignments;
    }

    public List<String> getAssignedEmployees(Day day, Shift shift) {
        return assignments.get(day).get(shift);
    }

    public void assignEmployee(Day day, Shift shift, String employeeName) {
        assignments.get(day).get(shift).add(employeeName);
    }

    public boolean isEmployeeScheduled(Day day, String employeeName) {
        Map<Shift, List<String>> daySchedule = assignments.get(day);
        for (List<String> employees : daySchedule.values()) {
            if (employees.contains(employeeName)) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        for (Day day : Day.values()) {
            for (Shift shift : Shift.values()) {
                assignments.get(day).get(shift).clear();
            }
        }
    }
}
