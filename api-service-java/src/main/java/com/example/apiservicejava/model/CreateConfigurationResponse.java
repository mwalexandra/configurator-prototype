package com.example.apiservicejava.model;

public class CreateConfigurationResponse {

    private String configId;
    private String status;

    public CreateConfigurationResponse() {
    }

    public CreateConfigurationResponse(String configId, String status) {
        this.configId = configId;
        this.status = status;
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
}