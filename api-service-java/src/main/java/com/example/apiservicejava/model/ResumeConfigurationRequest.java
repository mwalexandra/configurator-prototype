package com.example.apiservicejava.model;

public class ResumeConfigurationRequest {

    private String configurationId;
    private ConfigurationSnapshot snapshot;
    private String sourceContext;

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public ConfigurationSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(ConfigurationSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public String getSourceContext() {
        return sourceContext;
    }

    public void setSourceContext(String sourceContext) {
        this.sourceContext = sourceContext;
    }
}