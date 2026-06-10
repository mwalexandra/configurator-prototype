package com.example.apiservicejava.model;

public class PatchConfigurationRequest {

    private String characteristicId;
    private String value;

    public PatchConfigurationRequest() {
    }

    public String getCharacteristicId() {
        return characteristicId;
    }

    public void setCharacteristicId(String characteristicId) {
        this.characteristicId = characteristicId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}