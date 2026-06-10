package com.example.apiservicejava.model.sapkb;

import java.util.List;

public class SapKbProduct {

    private String id;
    private String name;
    private String description;
    private Boolean configurable;
    private Boolean isRoot;
    private List<SapKbCharacteristicGroup> characteristicGroups;

    public SapKbProduct() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getIsRoot() {
        return isRoot;
    }

    public void setIsRoot(Boolean root) {
        isRoot = root;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getConfigurable() {
        return configurable;
    }

    public void setConfigurable(Boolean configurable) {
        this.configurable = configurable;
    }

    public List<SapKbCharacteristicGroup> getCharacteristicGroups() {
        return characteristicGroups;
    }

    public void setCharacteristicGroups(List<SapKbCharacteristicGroup> characteristicGroups) {
        this.characteristicGroups = characteristicGroups;
    }
}