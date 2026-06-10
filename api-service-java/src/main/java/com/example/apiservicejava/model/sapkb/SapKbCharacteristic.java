package com.example.apiservicejava.model.sapkb;

import java.util.List;

public class SapKbCharacteristic {

    private String id;
    private String type;
    private Boolean multiValued;
    private Integer numberDecimals;
    private String name;
    private String description;
    private String entryFieldMask;
    private Integer length;
    private List<SapKbPossibleValue> possibleValues;

    public SapKbCharacteristic() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getMultiValued() {
        return multiValued;
    }

    public void setMultiValued(Boolean multiValued) {
        this.multiValued = multiValued;
    }

    public Integer getNumberDecimals() {
        return numberDecimals;
    }

    public void setNumberDecimals(Integer numberDecimals) {
        this.numberDecimals = numberDecimals;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEntryFieldMask() {
        return entryFieldMask;
    }

    public void setEntryFieldMask(String entryFieldMask) {
        this.entryFieldMask = entryFieldMask;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public List<SapKbPossibleValue> getPossibleValues() {
        return possibleValues;
    }

    public void setPossibleValues(List<SapKbPossibleValue> possibleValues) {
        this.possibleValues = possibleValues;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}