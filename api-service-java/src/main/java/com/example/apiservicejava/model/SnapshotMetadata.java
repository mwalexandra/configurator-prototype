package com.example.apiservicejava.model;

public class SnapshotMetadata {

    private String sourceContext;
    private String hostEntityType;
    private String hostEntityId;
    private String version;
    private String locale;

    public String getSourceContext() {
        return sourceContext;
    }

    public void setSourceContext(String sourceContext) {
        this.sourceContext = sourceContext;
    }

    public String getHostEntityType() {
        return hostEntityType;
    }

    public void setHostEntityType(String hostEntityType) {
        this.hostEntityType = hostEntityType;
    }

    public String getHostEntityId() {
        return hostEntityId;
    }

    public void setHostEntityId(String hostEntityId) {
        this.hostEntityId = hostEntityId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}