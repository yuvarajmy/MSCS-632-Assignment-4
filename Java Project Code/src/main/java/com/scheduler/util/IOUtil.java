package com.scheduler.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.scheduler.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class IOUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new Jdk8Module());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static List<Employee> importEmployees(File file) throws IOException {
        Employee[] employees = mapper.readValue(file, Employee[].class);
        return List.of(employees);
    }

    public static void exportEmployees(List<Employee> employees, File file) throws IOException {
        mapper.writeValue(file, employees);
    }

    public static void exportScheduleJSON(SchedulingResult result, File file) throws IOException {
        mapper.writeValue(file, result);
    }

    public static void exportScheduleCSV(Schedule schedule, File file) throws IOException {
        StringBuilder csv = new StringBuilder();

        csv.append("Day,Shift,Employees\n");

        for (Day day : Day.values()) {
            for (Shift shift : Shift.values()) {
                List<String> employees = schedule.getAssignedEmployees(day, shift);
                String employeeList = String.join("; ", employees);
                csv.append(String.format("%s,%s,\"%s\"\n", day, shift, employeeList));
            }
        }

        Files.write(file.toPath(), csv.toString().getBytes());
    }

    public static List<Employee> loadSampleData() {
        try {
            var resource = IOUtil.class.getResourceAsStream("/employees_sample.json");
            if (resource == null) {
                throw new IOException("Sample data file not found");
            }
            Employee[] employees = mapper.readValue(resource, Employee[].class);
            return List.of(employees);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sample data", e);
        }
    }
}
