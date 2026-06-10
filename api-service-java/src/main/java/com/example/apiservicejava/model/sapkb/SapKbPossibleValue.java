package com.example.apiservicejava.model.sapkb;

public class SapKbPossibleValue {

    private String id;
    private String name;
    private String description;
    private String valueLow;
    private String intervalType;

    public SapKbPossibleValue() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValueLow() {
        return valueLow;
    }

    public void setValueLow(String valueLow) {
        this.valueLow = valueLow;
    }

    public String getIntervalType() {
        return intervalType;
    }

    public void setIntervalType(String intervalType) {
        this.intervalType = intervalType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}