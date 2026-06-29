package com.example.apiservicejava.model.api;

public class ExternalConfigurationCreateRequest {
    private String productId;
    private String kbId;
    private ExternalConfigurationDto externalConfiguration;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public ExternalConfigurationDto getExternalConfiguration() {
        return externalConfiguration;
    }

    public void setExternalConfiguration(ExternalConfigurationDto externalConfiguration) {
        this.externalConfiguration = externalConfiguration;
    }
}