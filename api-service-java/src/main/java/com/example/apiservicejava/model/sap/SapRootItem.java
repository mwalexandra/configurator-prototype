package com.example.apiservicejava.model.sap;

import java.util.List;

public class SapRootItem {
    private String id;     // "1" — нужен для PATCH URL
    private String key;    // "CPS_BURGER"
    private String type;   // "MARA"
    private boolean complete;
    private boolean consistent;
    private List<SapCharacteristicGroup> characteristicGroups;
    private List<SapCharacteristic> characteristics;
    private List<Object> subItems;
    // getters/setters...
}