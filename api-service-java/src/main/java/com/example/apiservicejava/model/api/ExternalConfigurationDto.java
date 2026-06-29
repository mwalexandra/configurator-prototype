package com.example.apiservicejava.model.api;

public class ExternalConfigurationDto {
    private ExternalConfigurationItemDto rootItem;
    private ExternalConfigurationMetadataDto metadata;

    public ExternalConfigurationItemDto getRootItem() {
        return rootItem;
    }

    public void setRootItem(ExternalConfigurationItemDto rootItem) {
        this.rootItem = rootItem;
    }

    public ExternalConfigurationMetadataDto getMetadata() {
        return metadata;
    }

    public void setMetadata(ExternalConfigurationMetadataDto metadata) {
        this.metadata = metadata;
    }
}