package com.example.apiservicejava.model.api;

import java.util.ArrayList;
import java.util.List;

public class CharacteristicDto {

    private String id;
    private String name;
    private String description;
    private String valueType;
    private boolean required;
    private boolean visible;
    private boolean readOnly;
    private boolean complete;
    private boolean consistent;
    private Integer length;
    private Integer numberDecimals;
    private String entryFieldMask;
    private List<CharacteristicValueDto> values = new ArrayList<>();
    private List<CharacteristicValueDto> possibleValues = new ArrayList<>();

    public CharacteristicDto() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
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

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getNumberDecimals() {
        return numberDecimals;
    }

    public void setNumberDecimals(Integer numberDecimals) {
        this.numberDecimals = numberDecimals;
    }

    public String getEntryFieldMask() {
        return entryFieldMask;
    }

    public void setEntryFieldMask(String entryFieldMask) {
        this.entryFieldMask = entryFieldMask;
    }

    public List<CharacteristicValueDto> getValues() {
        return values;
    }

    public void setValues(List<CharacteristicValueDto> values) {
        this.values = values;
    }

    public List<CharacteristicValueDto> getPossibleValues() {
        return possibleValues;
    }

    public void setPossibleValues(List<CharacteristicValueDto> possibleValues) {
        this.possibleValues = possibleValues;
    }
}