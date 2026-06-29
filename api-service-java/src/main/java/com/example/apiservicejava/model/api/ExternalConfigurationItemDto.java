package com.example.apiservicejava.model.api;

import java.util.List;

public class ExternalConfigurationItemDto {
    private String id;
    private String key;
    private List<ExternalConfigurationCharacteristicDto> characteristics;
    private List<ExternalConfigurationItemDto> subItems;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<ExternalConfigurationCharacteristicDto> getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(List<ExternalConfigurationCharacteristicDto> characteristics) {
        this.characteristics = characteristics;
    }

    public List<ExternalConfigurationItemDto> getSubItems() {
        return subItems;
    }

    public void setSubItems(List<ExternalConfigurationItemDto> subItems) {
        this.subItems = subItems;
    }
}