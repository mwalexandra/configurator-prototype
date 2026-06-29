package com.example.apiservicejava.model.api;

import java.util.List;

public class DeleteConfigurationsRequest {
    private List<String> configurationIds;

    public DeleteConfigurationsRequest() {
    }

    public DeleteConfigurationsRequest(List<String> configurationIds) {
        this.configurationIds = configurationIds;
    }

    public List<String> getConfigurationIds() {
        return configurationIds;
    }

    public void setConfigurationIds(List<String> configurationIds) {
        this.configurationIds = configurationIds;
    }
}
