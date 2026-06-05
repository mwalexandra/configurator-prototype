package com.example.apiservicejava.model;

public class PatchConfigurationResponse {

    private String configId;
    private String status;

    public PatchConfigurationResponse() {
    }

    public PatchConfigurationResponse(String configId, String status) {
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