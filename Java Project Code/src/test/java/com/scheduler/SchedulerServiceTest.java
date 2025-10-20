package com.scheduler;

import com.scheduler.model.*;
import com.scheduler.service.SchedulerService;
import com.scheduler.util.IOUtil;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerServiceTest {

    @Test
    void testNoEmployeeExceedsFiveDays() {
        List<Employee> employees = IOUtil.loadSampleData();
        SchedulerService service = new SchedulerService(42);
        SchedulingResult result = service.generateSchedule(employees);

        Map<String, Integer> workDays = new HashMap<>();
        for (Employee emp : employees) {
            workDays.put(emp.getName(), 0);
        }

        Schedule schedule = result.getSchedule();
        for (Day day : Day.values()) {
            for (Shift shift : Shift.values()) {
                List<String> assigned = schedule.getAssignedEmployees(day, shift);
                for (String empName : assigned) {
                    workDays.put(empName, workDays.getOrDefault(empName, 0) + 1);
                }
            }
        }

        for (Map.Entry<String, Integer> entry : workDays.entrySet()) {
            assertTrue(entry.getValue() <= 5,
                String.format("Employee %s works %d days, exceeding maximum of 5",
                    entry.getKey(), entry.getValue()));
        }
    }

    @Test
    void testNoEmployeeHasMultipleShiftsPerDay() {
        List<Employee> employees = IOUtil.loadSampleData();
        SchedulerService service = new SchedulerService(42);
        SchedulingResult result = service.generateSchedule(employees);

        Schedule schedule = result.getSchedule();
        for (Day day : Day.values()) {
            Map<String, Integer> employeeShiftsOnDay = new HashMap<>();

            for (Shift shift : Shift.values()) {
                List<String> assigned = schedule.getAssignedEmployees(day, shift);
                for (String empName : assigned) {
                    employeeShiftsOnDay.put(empName, employeeShiftsOnDay.getOrDefault(empName, 0) + 1);
                }
            }

            for (Map.Entry<String, Integer> entry : employeeShiftsOnDay.entrySet()) {
                assertEquals(1, entry.getValue(),
                    String.format("Employee %s has %d shifts on %s, should have at most 1",
                        entry.getKey(), entry.getValue(), day));
            }
        }
    }

    @Test
    void testEveryShiftHasAtLeastTwoEmployees() {
        List<Employee> employees = IOUtil.loadSampleData();
        SchedulerService service = new SchedulerService(42);
        SchedulingResult result = service.generateSchedule(employees);

        Schedule schedule = result.getSchedule();
        for (Day day : Day.values()) {
            for (Shift shift : Shift.values()) {
                List<String> assigned = schedule.getAssignedEmployees(day, shift);
                assertTrue(assigned.size() >= 2,
                    String.format("Shift %s %s has only %d employees, needs at least 2",
                        day, shift, assigned.size()));
            }
        }
    }

    @Test
    void testDeterministicScheduling() {
        List<Employee> employees = IOUtil.loadSampleData();

        SchedulerService service1 = new SchedulerService(42);
        SchedulingResult result1 = service1.generateSchedule(employees);

        SchedulerService service2 = new SchedulerService(42);
        SchedulingResult result2 = service2.generateSchedule(employees);

        Schedule schedule1 = result1.getSchedule();
        Schedule schedule2 = result2.getSchedule();

        for (Day day : Day.values()) {
            for (Shift shift : Shift.values()) {
                List<String> assigned1 = schedule1.getAssignedEmployees(day, shift);
                List<String> assigned2 = schedule2.getAssignedEmployees(day, shift);

                assertEquals(assigned1.size(), assigned2.size(),
                    String.format("Different number of employees for %s %s", day, shift));

                assertTrue(assigned1.containsAll(assigned2) && assigned2.containsAll(assigned1),
                    String.format("Different employees assigned for %s %s", day, shift));
            }
        }
    }

    @Test
    void testStatsCalculation() {
        List<Employee> employees = IOUtil.loadSampleData();
        SchedulerService service = new SchedulerService(42);
        SchedulingResult result = service.generateSchedule(employees);

        Map<String, Double> stats = result.getStats();

        assertNotNull(stats.get("totalAssignments"));
        assertTrue(stats.get("totalAssignments") > 0);

        assertNotNull(stats.get("firstChoice"));
        assertNotNull(stats.get("secondChoice"));
        assertNotNull(stats.get("thirdChoice"));
        assertNotNull(stats.get("backfills"));

        assertNotNull(stats.get("firstChoicePct"));
        assertNotNull(stats.get("secondChoicePct"));
        assertNotNull(stats.get("thirdChoicePct"));

        assertTrue(stats.get("firstChoicePct") >= 0 && stats.get("firstChoicePct") <= 100);
        assertTrue(stats.get("secondChoicePct") >= 0 && stats.get("secondChoicePct") <= 100);
        assertTrue(stats.get("thirdChoicePct") >= 0 && stats.get("thirdChoicePct") <= 100);
    }

    @Test
    void testLogsGenerated() {
        List<Employee> employees = IOUtil.loadSampleData();
        SchedulerService service = new SchedulerService(42);
        SchedulingResult result = service.generateSchedule(employees);

        List<String> logs = result.getLogs();

        assertNotNull(logs);
        assertFalse(logs.isEmpty(), "Logs should not be empty");

        boolean hasPreferencePass = logs.stream().anyMatch(log -> log.contains("Preference Pass"));
        assertTrue(hasPreferencePass, "Logs should contain preference pass messages");
    }

    @Test
    void testEmptyEmployeeList() {
        SchedulerService service = new SchedulerService(42);
        SchedulingResult result = service.generateSchedule(List.of());

        Schedule schedule = result.getSchedule();
        for (Day day : Day.values()) {
            for (Shift shift : Shift.values()) {
                List<String> assigned = schedule.getAssignedEmployees(day, shift);
                assertTrue(assigned.isEmpty(),
                    String.format("Expected no assignments for %s %s with no employees", day, shift));
            }
        }
    }

    @Test
    void testSingleEmployeeNoPreferences() {
        Employee emp = new Employee("Test Employee");
        SchedulerService service = new SchedulerService(42);
        SchedulingResult result = service.generateSchedule(List.of(emp));

        Map<String, Integer> workDays = new HashMap<>();
        workDays.put(emp.getName(), 0);

        Schedule schedule = result.getSchedule();
        for (Day day : Day.values()) {
            for (Shift shift : Shift.values()) {
                List<String> assigned = schedule.getAssignedEmployees(day, shift);
                for (String empName : assigned) {
                    workDays.put(empName, workDays.getOrDefault(empName, 0) + 1);
                }
            }
        }

        assertTrue(workDays.get(emp.getName()) <= 5,
            "Single employee should not exceed 5 days");
    }
}
