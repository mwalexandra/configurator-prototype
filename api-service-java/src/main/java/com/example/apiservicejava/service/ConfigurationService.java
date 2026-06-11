package com.example.apiservicejava.service;

import com.example.apiservicejava.model.ConfigurationResponse;
import com.example.apiservicejava.model.CreateConfigurationRequest;
import com.example.apiservicejava.model.PatchConfigurationRequest;
import com.example.apiservicejava.model.sapkb.SapKbResponse;
import com.example.apiservicejava.model.sapruntime.SapRuntimeConfigurationResponse;
import com.example.apiservicejava.model.ConfigurationSnapshot;
import com.example.apiservicejava.model.RestoreInfo;
import com.example.apiservicejava.model.ResumeConfigurationRequest;
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

        if (runtimeResponse == null) {
            throw new IllegalStateException("SAP CPS returned null configuration body for configId=" + configId);
        }

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
        SapRuntimeConfigurationResponse currentRuntime = null;

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

        if (currentRuntime == null) {
            throw new IllegalStateException("SAP CPS returned null configuration body for configId=" + configId);
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

        if (updatedRuntime.getId() != null && refreshedRuntimeResult.getEtag() != null) {
            etagByConfigurationId.put(updatedRuntime.getId(), refreshedRuntimeResult.getEtag());
        }

        String kbId = updatedRuntime.getKbId() != null
                ? updatedRuntime.getKbId().toString()
                : null;

        SapKbResponse kbResponse = sapKbClient.getKnowledgeBase(kbId);

        ConfigurationResponse response = configurationMapper.toWidgetResponse(updatedRuntime, kbResponse);
        response.setBackendProcessingTimeMs(System.currentTimeMillis() - start);
        return response;
    }

    public ConfigurationResponse resumeConfiguration(ResumeConfigurationRequest request) {
        long start = System.currentTimeMillis();

        if (request.getConfigurationId() != null && !request.getConfigurationId().isBlank()) {
            try {
                ConfigurationResponse liveResponse = getConfiguration(request.getConfigurationId());

                RestoreInfo restoreInfo = new RestoreInfo();
                restoreInfo.setMode("resume");
                restoreInfo.setStatus("RESUMED");
                restoreInfo.setStrategy("LIVE_CONFIGURATION");
                restoreInfo.setLiveSessionAvailable(true);
                restoreInfo.setSnapshotUsed(false);
                restoreInfo.setReadOnly(false);
                restoreInfo.setMessage("Configuration restored from live CPS runtime");

                liveResponse.setRestoreInfo(restoreInfo);
                liveResponse.setBackendProcessingTimeMs(System.currentTimeMillis() - start);
                return liveResponse;
            } catch (Exception ex) {
                if (request.getSnapshot() == null) {
                    throw ex;
                }
            }
        }

        if (request.getSnapshot() != null) {
            ConfigurationSnapshot snapshot = request.getSnapshot();

            ConfigurationResponse response = new ConfigurationResponse();
            response.setConfigurationId(snapshot.getConfigurationId());
            response.setProductId(snapshot.getProductId());
            response.setKbId(snapshot.getKbId());
            response.setComplete(snapshot.isComplete());
            response.setConsistent(snapshot.isConsistent());
            response.setRootItem(snapshot.getRootItem());
            response.setGroups(snapshot.getGroups() != null ? snapshot.getGroups() : java.util.List.of());
            response.setMessages(snapshot.getMessages() != null ? snapshot.getMessages() : java.util.List.of());

            RestoreInfo restoreInfo = new RestoreInfo();
            restoreInfo.setMode("resume");
            restoreInfo.setStatus("FALLBACK_APPLIED");
            restoreInfo.setStrategy("READ_ONLY_SNAPSHOT");
            restoreInfo.setLiveSessionAvailable(false);
            restoreInfo.setSnapshotUsed(true);
            restoreInfo.setReadOnly(true);
            restoreInfo.setMessage("Configuration restored from snapshot fallback");

            response.setRestoreInfo(restoreInfo);
            response.setBackendProcessingTimeMs(System.currentTimeMillis() - start);

            return response;
        }

        throw new IllegalArgumentException("Resume requires configurationId or snapshot");
    }
}