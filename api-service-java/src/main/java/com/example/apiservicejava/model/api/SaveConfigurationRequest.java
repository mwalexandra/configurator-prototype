package com.example.apiservicejava.model.api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class SaveConfigurationRequest {

    private String id;
    private String label;
    private String productId;
    private String configurationId;

    @NotNull
    @Valid
    private ConfigurationSnapshot snapshot;

    public SaveConfigurationRequest() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

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
}