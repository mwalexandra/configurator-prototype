package com.example.apiservicejava.model.api;

public class CreateConfigurationRequest {

    private String productId;
    private String kbId;

    public CreateConfigurationRequest() {
    }

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
}