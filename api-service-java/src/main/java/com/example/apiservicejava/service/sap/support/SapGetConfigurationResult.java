package com.example.apiservicejava.service.sap.support;

import com.example.apiservicejava.model.sapruntime.SapRuntimeConfigurationResponse;

public class SapGetConfigurationResult {
    private SapRuntimeConfigurationResponse body;
    private String etag;

    public SapRuntimeConfigurationResponse getBody() {
        return body;
    }

    public void setBody(SapRuntimeConfigurationResponse body) {
        this.body = body;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }
}