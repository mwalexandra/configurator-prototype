package com.example.apiservicejava.service;

import com.example.apiservicejava.model.ConfigurationResponse;
import com.example.apiservicejava.model.CreateConfigurationRequest;
import com.example.apiservicejava.model.PatchConfigurationRequest;
import com.example.apiservicejava.model.sapkb.SapKbResponse;
import com.example.apiservicejava.model.sapruntime.SapRuntimeConfigurationResponse;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationService {

    private final SapCpsClient sapCpsClient;
    private final SapKbClient sapKbClient;
    private final ConfigurationMapper configurationMapper;

    public ConfigurationService(
            SapCpsClient sapCpsClient,
            SapKbClient sapKbClient,
            ConfigurationMapper configurationMapper
    ) {
        this.sapCpsClient = sapCpsClient;
        this.sapKbClient = sapKbClient;
        this.configurationMapper = configurationMapper;
    }

    public ConfigurationResponse createConfiguration(CreateConfigurationRequest request) {
        SapRuntimeConfigurationResponse runtimeResponse = sapCpsClient.createConfiguration(
                request.getProductId(),
                request.getKbId(),
                request.getLocale()
        );

        String kbId = runtimeResponse.getKbId() != null
                ? runtimeResponse.getKbId().toString()
                : request.getKbId();

        SapKbResponse kbResponse = sapKbClient.getKnowledgeBase(kbId);

        return configurationMapper.toWidgetResponse(runtimeResponse, kbResponse);
    }

    public ConfigurationResponse getConfiguration(String configId) {
        SapRuntimeConfigurationResponse runtimeResponse = sapCpsClient.getConfiguration(configId);

        String kbId = runtimeResponse.getKbId() != null
                ? runtimeResponse.getKbId().toString()
                : null;

        SapKbResponse kbResponse = sapKbClient.getKnowledgeBase(kbId);

        return configurationMapper.toWidgetResponse(runtimeResponse, kbResponse);
    }

    public ConfigurationResponse patchConfiguration(
            String configId,
            PatchConfigurationRequest request
    ) {
        SapRuntimeConfigurationResponse currentRuntime = sapCpsClient.getConfiguration(configId);

        String itemId = currentRuntime.getRootItem() != null
                ? currentRuntime.getRootItem().getId()
                : null;

        if (itemId == null || itemId.isBlank()) {
            throw new IllegalStateException("rootItem.id is missing in SAP runtime configuration");
        }

        SapRuntimeConfigurationResponse updatedRuntime = sapCpsClient.patchConfiguration(
                configId,
                itemId,
                request.getCharacteristicId(),
                request.getValue()
        );

        String kbId = updatedRuntime.getKbId() != null
                ? updatedRuntime.getKbId().toString()
                : null;

        SapKbResponse kbResponse = sapKbClient.getKnowledgeBase(kbId);

        return configurationMapper.toWidgetResponse(updatedRuntime, kbResponse);
    }
}