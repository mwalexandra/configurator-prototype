package com.example.apiservicejava.model.sapkb;

import java.util.List;

public class SapKbCharacteristicGroup {

    private String id;
    private String name;
    private List<String> characteristicIDs;

    public SapKbCharacteristicGroup() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getCharacteristicIDs() {
        return characteristicIDs;
    }

    public void setCharacteristicIDs(List<String> characteristicIDs) {
        this.characteristicIDs = characteristicIDs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}