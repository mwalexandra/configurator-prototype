package com.example.apiservicejava.model.sap;

import java.util.List;

public class SapConfigurationResponse {
    private String id;
    private Integer kbId;
    private SapKbKey kbKey;
    private boolean complete;
    private boolean consistent;
    private boolean locked;
    private String productKey;
    private String productType;
    private SapRootItem rootItem;
    private List<SapConflict> conflicts;
    
    
    // getters/setters...
}