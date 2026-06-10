package com.example.apiservicejava.model.sapruntime;

public class SapRuntimePossibleValue {

    private String valueLow;
    private boolean selectable;
    private String intervalType;

    public SapRuntimePossibleValue() {
    }

    public String getValueLow() {
        return valueLow;
    }

    public void setValueLow(String valueLow) {
        this.valueLow = valueLow;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public String getIntervalType() {
        return intervalType;
    }

    public void setIntervalType(String intervalType) {
        this.intervalType = intervalType;
    }
}