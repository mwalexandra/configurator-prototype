package com.example.apiservicejava.service;

public class SapConfigurationContext {
    private String configurationId;
    private String etag;

    public SapConfigurationContext() {
    }

    public SapConfigurationContext(String configurationId, String etag) {
        this.configurationId = configurationId;
        this.etag = etag;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }
}