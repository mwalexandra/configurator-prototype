package com.example.apiservicejava.model.sapkb;

public class SapKbHeaderInfo {

    private Integer id;
    private SapKbKey key;

    public SapKbHeaderInfo() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public SapKbKey getKey() {
        return key;
    }

    public void setKey(SapKbKey key) {
        this.key = key;
    }
}