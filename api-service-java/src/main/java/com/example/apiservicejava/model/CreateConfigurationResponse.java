package com.example.apiservicejava.model;

public class CreateConfigurationResponse {

    private String configId;
    private String status;
    private String configuration;
    private long responseTimeMs;

    public CreateConfigurationResponse() {
    }

    public CreateConfigurationResponse(String configId, String status, String configuration, long responseTimeMs) {
        this.configId = configId;
        this.status = status;
        this.configuration = configuration;
        this.responseTimeMs = responseTimeMs;
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

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
}