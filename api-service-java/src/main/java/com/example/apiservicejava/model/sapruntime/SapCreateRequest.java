package com.example.apiservicejava.model.sapruntime;

import java.util.List;

public class SapCreateRequest {

    private List<SapContextEntry> context;
    private String date;
    private Integer kbId;
    private String productKey;
    private SapSource source;

    public SapCreateRequest() {
    }

    public List<SapContextEntry> getContext() {
        return context;
    }

    public void setContext(List<SapContextEntry> context) {
        this.context = context;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getKbId() {
        return kbId;
    }

    public void setKbId(Integer kbId) {
        this.kbId = kbId;
    }

    public String getProductKey() {
        return productKey;
    }

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    public SapSource getSource() {
        return source;
    }

    public void setSource(SapSource source) {
        this.source = source;
    }

    public static class SapContextEntry {
        private String name;
        private String value;

        public SapContextEntry() {
        }

        public SapContextEntry(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class SapSource {
        private String application;
        private String type;
        private String id;

        public SapSource() {
        }

        public SapSource(String application, String type, String id) {
            this.application = application;
            this.type = type;
            this.id = id;
        }

        public String getApplication() {
            return application;
        }

        public void setApplication(String application) {
            this.application = application;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}