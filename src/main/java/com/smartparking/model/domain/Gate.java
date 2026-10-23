package com.smartparking.model.domain;

public abstract class Gate {
    private final String id;
    private final String name;

    protected Gate(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
