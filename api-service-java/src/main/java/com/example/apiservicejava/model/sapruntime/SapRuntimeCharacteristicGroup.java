package com.example.apiservicejava.model.sapruntime;

public class SapRuntimeCharacteristicGroup {

    private String id;
    private boolean consistent;
    private boolean complete;
    private boolean visible;

    public SapRuntimeCharacteristicGroup() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}