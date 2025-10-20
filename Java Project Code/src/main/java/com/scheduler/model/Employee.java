package com.scheduler.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Employee {
    private String id;
    private String name;
    private Map<Day, Preference> preferences;

    public Employee() {
        this.id = UUID.randomUUID().toString();
        this.preferences = new HashMap<>();
        for (Day day : Day.values()) {
            preferences.put(day, new Preference());
        }
    }

    public Employee(String name) {
        this();
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Day, Preference> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<Day, Preference> preferences) {
        this.preferences = preferences != null ? preferences : new HashMap<>();
    }

    public Preference getPreference(Day day) {
        return preferences.computeIfAbsent(day, k -> new Preference());
    }

    public void setPreference(Day day, Preference preference) {
        preferences.put(day, preference);
    }
}
