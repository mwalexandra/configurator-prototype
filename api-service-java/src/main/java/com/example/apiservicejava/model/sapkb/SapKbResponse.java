package com.example.apiservicejava.model.sapkb;

import java.util.List;

public class SapKbResponse {

    private SapKbHeaderInfo headerInfo;
    private String language;
    private List<SapKbProduct> products;
    private List<SapKbCharacteristic> characteristics;

    public SapKbResponse() {
    }

    public SapKbHeaderInfo getHeaderInfo() {
        return headerInfo;
    }

    public void setHeaderInfo(SapKbHeaderInfo headerInfo) {
        this.headerInfo = headerInfo;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<SapKbProduct> getProducts() {
        return products;
    }

    public void setProducts(List<SapKbProduct> products) {
        this.products = products;
    }

    public List<SapKbCharacteristic> getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(List<SapKbCharacteristic> characteristics) {
        this.characteristics = characteristics;
    }
}