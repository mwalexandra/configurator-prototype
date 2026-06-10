package com.example.apiservicejava.model;

public class CharacteristicGroup {

    private String id;
    private String name;
    private boolean complete;
    private boolean consistent;
    private boolean visible;

    public CharacteristicGroup() {
    }

    public CharacteristicGroup(String id, String name, boolean complete, boolean consistent, boolean visible) {
        this.id = id;
        this.name = name;
        this.complete = complete;
        this.consistent = consistent;
        this.visible = visible;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}