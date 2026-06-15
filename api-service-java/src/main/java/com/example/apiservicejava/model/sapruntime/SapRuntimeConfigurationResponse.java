package com.example.apiservicejava.model.sapruntime;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SapRuntimeConfigurationResponse {

    private String id;
    private Integer kbId;
    private SapRuntimeKbKey kbKey;
    private Integer kbBuild;

    @JsonProperty("consistent")
    private boolean consistent;

    @JsonProperty("complete")
    private boolean complete;
    
    private String engine;
    private String autoCleanup;
    private boolean locked;
    private SapRuntimeRootItem rootItem;
    private String productKey;
    private String productType;
    private List<SapRuntimeConflict> conflicts;

    public SapRuntimeConfigurationResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getKbId() {
        return kbId;
    }

    public void setKbId(Integer kbId) {
        this.kbId = kbId;
    }

    public SapRuntimeKbKey getKbKey() {
        return kbKey;
    }

    public void setKbKey(SapRuntimeKbKey kbKey) {
        this.kbKey = kbKey;
    }

    public Integer getKbBuild() {
        return kbBuild;
    }

    public void setKbBuild(Integer kbBuild) {
        this.kbBuild = kbBuild;
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

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getAutoCleanup() {
        return autoCleanup;
    }

    public void setAutoCleanup(String autoCleanup) {
        this.autoCleanup = autoCleanup;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public SapRuntimeRootItem getRootItem() {
        return rootItem;
    }

    public void setRootItem(SapRuntimeRootItem rootItem) {
        this.rootItem = rootItem;
    }

    public String getProductKey() {
        return productKey;
    }

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public List<SapRuntimeConflict> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<SapRuntimeConflict> conflicts) {
        this.conflicts = conflicts;
    }
}