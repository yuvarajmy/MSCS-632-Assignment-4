package com.scheduler.service;

import com.scheduler.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class SchedulerService {
    private static final int MIN_PER_SHIFT = 2;
    private static final int MAX_DAYS_PER_EMP = 5;

    private Random random;

    public SchedulerService(long seed) {
        this.random = new Random(seed);
    }

    public SchedulingResult generateSchedule(List<Employee> employees) {
        Schedule schedule = new Schedule();
        List<String> logs = new ArrayList<>();
        Map<String, Integer> workCounts = new HashMap<>();

        for (Employee emp : employees) {
            workCounts.put(emp.getName(), 0);
        }

        List<PlacementRequest> unresolvedRequests = new ArrayList<>();

        logs.add("=== Starting Preference Pass ===");

        for (Employee employee : employees) {
            for (Day day : Day.values()) {
                Preference pref = employee.getPreference(day);

                if (!pref.hasPreference()) {
                    continue;
                }

                PlacementRequest request = new PlacementRequest(employee.getName(), day, pref);

                if (pref.isRanked()) {
                    boolean placed = tryRankedPreferences(schedule, workCounts, request, logs);
                    if (!placed) {
                        unresolvedRequests.add(request);
                        logs.add(String.format("CONFLICT: %s could not be placed on %s (ranked preferences)",
                            employee.getName(), day));
                    }
                } else if (pref.isSingle()) {
                    boolean placed = trySinglePreference(schedule, workCounts, request, logs);
                    if (!placed) {
                        unresolvedRequests.add(request);
                        logs.add(String.format("CONFLICT: %s could not be placed on %s (single preference: %s)",
                            employee.getName(), day, pref.getSingle().orElse("NONE")));
                    }
                }
            }
        }

        logs.add("\n=== Same-Day Alternatives Pass ===");
        List<PlacementRequest> stillUnresolved = new ArrayList<>();

        for (PlacementRequest request : unresolvedRequests) {
            boolean placed = trySameDayAlternatives(schedule, workCounts, request, logs);
            if (!placed) {
                stillUnresolved.add(request);
                logs.add(String.format("Still unresolved: %s on %s", request.employeeName, request.day));
            }
        }

        logs.add("\n=== Next-Day Spillover Pass ===");
        for (PlacementRequest request : stillUnresolved) {
            boolean placed = tryNextDaySpillover(schedule, workCounts, request, logs);
            if (!placed) {
                logs.add(String.format("FAILED TO PLACE: %s (all days attempted)", request.employeeName));
            }
        }

        logs.add("\n=== Backfill Pass ===");
        backfillShifts(schedule, workCounts, employees, logs);

        Map<String, Double> stats = calculateStats(schedule, employees);

        return new SchedulingResult(schedule, stats, logs);
    }

    private boolean tryRankedPreferences(Schedule schedule, Map<String, Integer> workCounts,
                                        PlacementRequest request, List<String> logs) {
        Map<String, Integer> ranked = request.preference.getRanked();

        List<Map.Entry<String, Integer>> sortedPrefs = ranked.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .collect(Collectors.toList());

        for (Map.Entry<String, Integer> entry : sortedPrefs) {
            String shiftName = entry.getKey();
            Integer priority = entry.getValue();
            Shift shift = Shift.valueOf(shiftName.toUpperCase());

            if (canPlace(schedule, workCounts, request.employeeName, request.day, shift)) {
                place(schedule, workCounts, request.employeeName, request.day, shift);
                logs.add(String.format("Placed %s on %s %s (priority %d)",
                    request.employeeName, request.day, shift, priority));
                return true;
            }
        }

        return false;
    }

    private boolean trySinglePreference(Schedule schedule, Map<String, Integer> workCounts,
                                       PlacementRequest request, List<String> logs) {
        Optional<String> singleOpt = request.preference.getSingle();
        if (!singleOpt.isPresent()) {
            return false;
        }

        String shiftName = singleOpt.get();
        Shift shift = Shift.valueOf(shiftName.toUpperCase());

        if (canPlace(schedule, workCounts, request.employeeName, request.day, shift)) {
            place(schedule, workCounts, request.employeeName, request.day, shift);
            logs.add(String.format("Placed %s on %s %s (single preference)",
                request.employeeName, request.day, shift));
            return true;
        }

        return false;
    }

    private boolean trySameDayAlternatives(Schedule schedule, Map<String, Integer> workCounts,
                                          PlacementRequest request, List<String> logs) {
        for (Shift shift : Shift.values()) {
            if (canPlace(schedule, workCounts, request.employeeName, request.day, shift)) {
                place(schedule, workCounts, request.employeeName, request.day, shift);
                logs.add(String.format("Placed %s on %s %s (same-day alternative)",
                    request.employeeName, request.day, shift));
                return true;
            }
        }
        return false;
    }

    private boolean tryNextDaySpillover(Schedule schedule, Map<String, Integer> workCounts,
                                       PlacementRequest request, List<String> logs) {
        Day[] days = Day.values();
        int startIdx = request.day.ordinal();

        for (int i = 1; i < days.length; i++) {
            int nextIdx = (startIdx + i) % days.length;
            Day nextDay = days[nextIdx];

            for (Shift shift : Shift.values()) {
                if (canPlace(schedule, workCounts, request.employeeName, nextDay, shift)) {
                    place(schedule, workCounts, request.employeeName, nextDay, shift);
                    logs.add(String.format("Placed %s on %s %s (spillover from %s)",
                        request.employeeName, nextDay, shift, request.day));
                    return true;
                }
            }
        }

        return false;
    }

    private void backfillShifts(Schedule schedule, Map<String, Integer> workCounts,
                               List<Employee> employees, List<String> logs) {
        for (Day day : Day.values()) {
            for (Shift shift : Shift.values()) {
                List<String> assigned = schedule.getAssignedEmployees(day, shift);

                while (assigned.size() < MIN_PER_SHIFT) {
                    List<String> eligible = employees.stream()
                        .map(Employee::getName)
                        .filter(name -> !schedule.isEmployeeScheduled(day, name))
                        .filter(name -> workCounts.get(name) < MAX_DAYS_PER_EMP)
                        .collect(Collectors.toList());

                    if (eligible.isEmpty()) {
                        logs.add(String.format("WARNING: Cannot backfill %s %s - no eligible employees",
                            day, shift));
                        break;
                    }

                    String selected = eligible.get(random.nextInt(eligible.size()));
                    place(schedule, workCounts, selected, day, shift);
                    logs.add(String.format("BACKFILL: Added %s to %s %s", selected, day, shift));
                }
            }
        }
    }

    private boolean canPlace(Schedule schedule, Map<String, Integer> workCounts,
                             String employeeName, Day day, Shift shift) {
        // Do not place the same person twice in a day
        if (schedule.isEmployeeScheduled(day, employeeName)) {
            return false;
        }

        // NEW: avoid overfilling a shift during preference/spillover passes
        if (schedule.getAssignedEmployees(day, shift).size() >= MIN_PER_SHIFT) {
            return false;
        }

        // Respect the 5-days-per-employee limit
        if (workCounts.get(employeeName) >= MAX_DAYS_PER_EMP) {
            return false;
        }

        return true;
    }

    private void place(Schedule schedule, Map<String, Integer> workCounts,
                      String employeeName, Day day, Shift shift) {
        schedule.assignEmployee(day, shift, employeeName);
        workCounts.put(employeeName, workCounts.get(employeeName) + 1);
    }

    private Map<String, Double> calculateStats(Schedule schedule, List<Employee> employees) {
        Map<String, Double> stats = new HashMap<>();

        int totalAssignments = 0;
        int firstChoice = 0;
        int secondChoice = 0;
        int thirdChoice = 0;
        int backfills = 0;

        for (Day day : Day.values()) {
            for (Shift shift : Shift.values()) {
                List<String> assigned = schedule.getAssignedEmployees(day, shift);
                totalAssignments += assigned.size();

                for (String empName : assigned) {
                    Employee emp = employees.stream()
                        .filter(e -> e.getName().equals(empName))
                        .findFirst()
                        .orElse(null);

                    if (emp == null) {
                        continue;
                    }

                    Preference pref = emp.getPreference(day);

                    if (!pref.hasPreference()) {
                        backfills++;
                    } else if (pref.isSingle()) {
                        if (pref.getSingle().orElse("").equalsIgnoreCase(shift.name())) {
                            firstChoice++;
                        }
                    } else if (pref.isRanked()) {
                        Map<String, Integer> ranked = pref.getRanked();
                        Integer priority = ranked.get(shift.name().toUpperCase());
                        if (priority == null) {
                            priority = ranked.get(shift.name());
                        }

                        if (priority != null) {
                            if (priority == 1) {
                                firstChoice++;
                            } else if (priority == 2) {
                                secondChoice++;
                            } else if (priority == 3) {
                                thirdChoice++;
                            }
                        }
                    }
                }
            }
        }

        stats.put("totalAssignments", (double) totalAssignments);
        stats.put("firstChoice", (double) firstChoice);
        stats.put("secondChoice", (double) secondChoice);
        stats.put("thirdChoice", (double) thirdChoice);
        stats.put("backfills", (double) backfills);

        if (totalAssignments > 0) {
            stats.put("firstChoicePct", (firstChoice * 100.0) / totalAssignments);
            stats.put("secondChoicePct", (secondChoice * 100.0) / totalAssignments);
            stats.put("thirdChoicePct", (thirdChoice * 100.0) / totalAssignments);
        } else {
            stats.put("firstChoicePct", 0.0);
            stats.put("secondChoicePct", 0.0);
            stats.put("thirdChoicePct", 0.0);
        }

        return stats;
    }

    private static class PlacementRequest {
        String employeeName;
        Day day;
        Preference preference;

        PlacementRequest(String employeeName, Day day, Preference preference) {
            this.employeeName = employeeName;
            this.day = day;
            this.preference = preference;
        }
    }
}
