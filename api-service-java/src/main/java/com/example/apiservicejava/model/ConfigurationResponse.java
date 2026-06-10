package com.example.apiservicejava.model;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationResponse {

    private String configurationId;
    private String productId;
    private String kbId;
    private boolean complete;
    private boolean consistent;
    private ConfigurationItem rootItem;
    private List<CharacteristicGroupDto> groups = new ArrayList<>();
    private List<ConfigurationMessage> messages = new ArrayList<>();

    public ConfigurationResponse() {
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
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

    public ConfigurationItem getRootItem() {
        return rootItem;
    }

    public void setRootItem(ConfigurationItem rootItem) {
        this.rootItem = rootItem;
    }

    public List<CharacteristicGroupDto> getGroups() {
        return groups;
    }

    public void setGroups(List<CharacteristicGroupDto> groups) {
        this.groups = groups;
    }

    public List<ConfigurationMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ConfigurationMessage> messages) {
        this.messages = messages;
    }
}