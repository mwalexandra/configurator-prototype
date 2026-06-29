package com.example.apiservicejava.model.api;

import java.util.List;

public class DeleteConfigurationsResponse {
    private int totalRequested;
    private int successfullyDeleted;
    private List<String> failedConfigurationIds;

    public DeleteConfigurationsResponse() {
    }

    public DeleteConfigurationsResponse(int totalRequested, int successfullyDeleted, List<String> failedConfigurationIds) {
        this.totalRequested = totalRequested;
        this.successfullyDeleted = successfullyDeleted;
        this.failedConfigurationIds = failedConfigurationIds;
    }

    public int getTotalRequested() {
        return totalRequested;
    }

    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public int getSuccessfullyDeleted() {
        return successfullyDeleted;
    }

    public void setSuccessfullyDeleted(int successfullyDeleted) {
        this.successfullyDeleted = successfullyDeleted;
    }

    public List<String> getFailedConfigurationIds() {
        return failedConfigurationIds;
    }

    public void setFailedConfigurationIds(List<String> failedConfigurationIds) {
        this.failedConfigurationIds = failedConfigurationIds;
    }
}
