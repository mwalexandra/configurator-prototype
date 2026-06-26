package com.example.apiservicejava.model.api;

import java.util.List;

public class ExternalConfigurationCharacteristicDto {
    private String id;
    private List<ExternalConfigurationValueDto> values;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<ExternalConfigurationValueDto> getValues() {
        return values;
    }

    public void setValues(List<ExternalConfigurationValueDto> values) {
        this.values = values;
    }
}