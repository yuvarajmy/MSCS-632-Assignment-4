package com.scheduler.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchedulingResult {
    private Schedule schedule;
    private Map<String, Double> stats;
    private List<String> logs;

    public SchedulingResult() {
        this.schedule = new Schedule();
        this.stats = new HashMap<>();
        this.logs = new ArrayList<>();
    }

    public SchedulingResult(Schedule schedule, Map<String, Double> stats, List<String> logs) {
        this.schedule = schedule;
        this.stats = stats;
        this.logs = logs;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public Map<String, Double> getStats() {
        return stats;
    }

    public void setStats(Map<String, Double> stats) {
        this.stats = stats;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    public void addLog(String message) {
        this.logs.add(message);
    }
}
