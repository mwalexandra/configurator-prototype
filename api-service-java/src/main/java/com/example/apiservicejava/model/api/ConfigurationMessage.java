package com.example.apiservicejava.model.api;

public class ConfigurationMessage {

    private String severity;
    private String text;
    private String characteristicId;

    public ConfigurationMessage() {
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCharacteristicId() {
        return characteristicId;
    }

    public void setCharacteristicId(String characteristicId) {
        this.characteristicId = characteristicId;
    }
}