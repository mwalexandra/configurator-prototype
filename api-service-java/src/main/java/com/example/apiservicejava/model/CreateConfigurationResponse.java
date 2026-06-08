package com.example.apiservicejava.model;

public class CreateConfigurationResponse {

    private String configId;
    private String status;
    private String configuration;

    public CreateConfigurationResponse() {
    }

    public CreateConfigurationResponse(String configId, String status, String configuration) {
        this.configId = configId;
        this.status = status;
        this.configuration = configuration;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }
}