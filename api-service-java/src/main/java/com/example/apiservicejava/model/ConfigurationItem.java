package com.example.apiservicejava.model;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationItem {

    private String id;
    private String key;
    private boolean complete;
    private boolean consistent;
    private List<CharacteristicDto> characteristics = new ArrayList<>();
    private List<ConfigurationItem> subItems = new ArrayList<>();

    public ConfigurationItem() {
    }

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

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }

    public List<CharacteristicDto> getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(List<CharacteristicDto> characteristics) {
        this.characteristics = characteristics;
    }

    public List<ConfigurationItem> getSubItems() {
        return subItems;
    }

    public void setSubItems(List<ConfigurationItem> subItems) {
        this.subItems = subItems;
    }
}