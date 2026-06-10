package com.example.apiservicejava.service;

import com.example.apiservicejava.model.ConfigurationResponse;
import com.example.apiservicejava.model.CreateConfigurationRequest;
import com.example.apiservicejava.model.PatchConfigurationRequest;
import com.example.apiservicejava.model.sapkb.SapKbResponse;
import com.example.apiservicejava.model.sapruntime.SapRuntimeConfigurationResponse;
import org.springframework.stereotype.Service;

import com.example.apiservicejava.model.sapruntime.SapCreateRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConfigurationService {

    private final SapCpsClient sapCpsClient;
    private final SapKbClient sapKbClient;
    private final ConfigurationMapper configurationMapper;
    private final Map<String, String> etagByConfigurationId = new ConcurrentHashMap<>();

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
        long start = System.currentTimeMillis();

        // create SAP runtime configuration
        SapCreateRequest sapRequest = new SapCreateRequest();
        sapRequest.setProductKey(request.getProductId());

        if (request.getKbId() != null && !request.getKbId().isBlank()) {
            sapRequest.setKbId(Integer.valueOf(request.getKbId()));
        }

        sapRequest.setDate("2018-08-09");
        sapRequest.setContext(java.util.List.of(
                new SapCreateRequest.SapContextEntry("VBAP-VRKME", "EA")
        ));
        sapRequest.setSource(
                new SapCreateRequest.SapSource("cpq", "quote_item", "10")
        );

        SapRuntimeConfigurationResponse runtimeResponse = sapCpsClient.createConfiguration(sapRequest);

        String kbId = runtimeResponse.getKbId() != null
                ? runtimeResponse.getKbId().toString()
                : request.getKbId();

        SapKbResponse kbResponse = sapKbClient.getKnowledgeBase(kbId);

        ConfigurationResponse response = configurationMapper.toWidgetResponse(runtimeResponse, kbResponse);
        response.setBackendProcessingTimeMs(System.currentTimeMillis() - start);
        return response;
    }

    public ConfigurationResponse getConfiguration(String configId) {
        long start = System.currentTimeMillis();

        SapGetConfigurationResult runtimeResult = sapCpsClient.getConfigurationWithEtag(configId);
        SapRuntimeConfigurationResponse runtimeResponse = runtimeResult.getBody();

        if (runtimeResponse != null && runtimeResponse.getId() != null && runtimeResult.getEtag() != null) {
            etagByConfigurationId.put(runtimeResponse.getId(), runtimeResult.getEtag());
        }

        String kbId = runtimeResponse.getKbId() != null
                ? runtimeResponse.getKbId().toString()
                : null;

        SapKbResponse kbResponse = sapKbClient.getKnowledgeBase(kbId);

        ConfigurationResponse response = configurationMapper.toWidgetResponse(runtimeResponse, kbResponse);
        response.setBackendProcessingTimeMs(System.currentTimeMillis() - start);
        return response;
    }

    public ConfigurationResponse patchConfiguration(
            String configId,
            PatchConfigurationRequest request
    ) {
        long start = System.currentTimeMillis();

        String etag = etagByConfigurationId.get(configId);
        SapRuntimeConfigurationResponse currentRuntime;

        if (etag == null) {
            SapGetConfigurationResult runtimeResult = sapCpsClient.getConfigurationWithEtag(configId);
            currentRuntime = runtimeResult.getBody();

            if (currentRuntime != null && currentRuntime.getId() != null && runtimeResult.getEtag() != null) {
                etagByConfigurationId.put(currentRuntime.getId(), runtimeResult.getEtag());
                etag = runtimeResult.getEtag();
            }
        } else {
            currentRuntime = sapCpsClient.getConfiguration(configId);
        }

        String itemId = currentRuntime.getRootItem() != null
                ? currentRuntime.getRootItem().getId()
                : null;

        if (itemId == null || itemId.isBlank()) {
            throw new IllegalStateException("rootItem.id is missing in SAP runtime configuration");
        }

        sapCpsClient.patchConfiguration(
                configId,
                itemId,
                request.getCharacteristicId(),
                request.getValue(),
                etag
        );

        SapGetConfigurationResult refreshedRuntimeResult = sapCpsClient.getConfigurationWithEtag(configId);
        SapRuntimeConfigurationResponse updatedRuntime = refreshedRuntimeResult.getBody();

        if (updatedRuntime == null) {
            throw new IllegalStateException(
                    "SAP CPS patchConfiguration returned null body for configurationId=" + configId
                            + ", characteristicId=" + request.getCharacteristicId()
            );
        }

        if (refreshedRuntimeResult.getBody() != null
                && refreshedRuntimeResult.getBody().getId() != null
                && refreshedRuntimeResult.getEtag() != null) {
            etagByConfigurationId.put(
                    refreshedRuntimeResult.getBody().getId(),
                    refreshedRuntimeResult.getEtag()
            );
        }

        String kbId = updatedRuntime.getKbId() != null
                ? updatedRuntime.getKbId().toString()
                : null;

        SapKbResponse kbResponse = sapKbClient.getKnowledgeBase(kbId);

        ConfigurationResponse response = configurationMapper.toWidgetResponse(updatedRuntime, kbResponse);
        response.setBackendProcessingTimeMs(System.currentTimeMillis() - start);
        return response;
    }
}