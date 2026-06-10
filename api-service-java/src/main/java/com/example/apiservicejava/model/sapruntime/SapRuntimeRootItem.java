package com.example.apiservicejava.model.sapruntime;

import java.util.List;

public class SapRuntimeRootItem {

    private String id;
    private String key;
    private String type;
    private boolean salesRelevant;
    private boolean consistent;
    private boolean complete;
    private List<SapRuntimeCharacteristicGroup> characteristicGroups;
    private List<SapRuntimeCharacteristic> characteristics;
    private List<Object> variantConditions;
    private List<Object> subItems;

    public SapRuntimeRootItem() {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSalesRelevant() {
        return salesRelevant;
    }

    public void setSalesRelevant(boolean salesRelevant) {
        this.salesRelevant = salesRelevant;
    }

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public List<SapRuntimeCharacteristicGroup> getCharacteristicGroups() {
        return characteristicGroups;
    }

    public void setCharacteristicGroups(List<SapRuntimeCharacteristicGroup> characteristicGroups) {
        this.characteristicGroups = characteristicGroups;
    }

    public List<SapRuntimeCharacteristic> getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(List<SapRuntimeCharacteristic> characteristics) {
        this.characteristics = characteristics;
    }

    public List<Object> getVariantConditions() {
        return variantConditions;
    }

    public void setVariantConditions(List<Object> variantConditions) {
        this.variantConditions = variantConditions;
    }

    public List<Object> getSubItems() {
        return subItems;
    }

    public void setSubItems(List<Object> subItems) {
        this.subItems = subItems;
    }
}