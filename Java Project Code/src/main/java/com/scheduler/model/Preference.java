package com.scheduler.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Preference {
    private String single;
    private Map<String, Integer> ranked;

    public Preference() {
        this.ranked = new HashMap<>();
    }

    public Optional<String> getSingle() {
        return Optional.ofNullable(single);
    }

    public void setSingle(String single) {
        this.single = single;
        if (single != null) {
            this.ranked.clear();
        }
    }

    public Map<String, Integer> getRanked() {
        return ranked;
    }

    public void setRanked(Map<String, Integer> ranked) {
        this.ranked = ranked != null ? ranked : new HashMap<>();
        if (this.ranked != null && !this.ranked.isEmpty()) {
            this.single = null;
        }
    }

    public boolean hasPreference() {
        return single != null || (ranked != null && !ranked.isEmpty());
    }

    public boolean isRanked() {
        return ranked != null && !ranked.isEmpty();
    }

    public boolean isSingle() {
        return single != null;
    }
}
