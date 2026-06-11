package com.example.apiservicejava.model.api;

public class CharacteristicValueDto {

    private String id;
    private String name;
    private String description;
    private boolean selected;
    private String author;

    public CharacteristicValueDto() {
    }

    public CharacteristicValueDto(String id, String name, boolean selected) {
        this.id = id;
        this.name = name;
        this.selected = selected;
    }

    public CharacteristicValueDto(String id, String name, boolean selected, String author) {
        this.id = id;
        this.name = name;
        this.selected = selected;
        this.author = author;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}