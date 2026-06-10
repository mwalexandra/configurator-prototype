package com.example.apiservicejava.model.sapruntime;

import java.util.List;

public class SapRuntimeCharacteristic {

    private String id;
    private boolean readOnly;
    private boolean required;
    private boolean visible;
    private boolean consistent;
    private boolean complete;
    private List<SapRuntimePossibleValue> possibleValues;
    private List<SapRuntimeValue> values;

    public SapRuntimeCharacteristic() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public List<SapRuntimePossibleValue> getPossibleValues() {
        return possibleValues;
    }

    public void setPossibleValues(List<SapRuntimePossibleValue> possibleValues) {
        this.possibleValues = possibleValues;
    }

    public List<SapRuntimeValue> getValues() {
        return values;
    }

    public void setValues(List<SapRuntimeValue> values) {
        this.values = values;
    }
}