package com.example.apiservicejava.service;

import com.example.apiservicejava.mapper.ConfigurationMapper;
import com.example.apiservicejava.mapper.ExternalConfigurationMapper;
import com.example.apiservicejava.model.api.ConfigurationResponse;
import com.example.apiservicejava.model.api.ConfigurationSnapshot;
import com.example.apiservicejava.model.api.CreateConfigurationRequest;
import com.example.apiservicejava.model.api.DeleteConfigurationsRequest;
import com.example.apiservicejava.model.api.DeleteConfigurationsResponse;
import com.example.apiservicejava.model.api.PatchConfigurationRequest;
import com.example.apiservicejava.model.api.RestoreInfo;
import com.example.apiservicejava.model.api.ResumeConfigurationRequest;
import com.example.apiservicejava.model.api.ExternalConfigurationCreateRequest;
import com.example.apiservicejava.model.sapkb.SapKbResponse;
import com.example.apiservicejava.model.sapruntime.SapCreateRequest;
import com.example.apiservicejava.model.sapruntime.SapRuntimeConfigurationResponse;
import com.example.apiservicejava.service.sap.SapCpsClient;
import com.example.apiservicejava.service.sap.support.SapGetConfigurationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationService.class);

    private final SapCpsClient sapCpsClient;
    private final SapKbClient sapKbClient;
    private final ConfigurationMapper configurationMapper;

    private final Map<String, String> etagByConfigurationId = new ConcurrentHashMap<>();
    private final Map<String, Boolean> readOnlyByConfigurationId = new ConcurrentHashMap<>();

    public ConfigurationService(
            SapCpsClient sapCpsClient,
            SapKbClient sapKbClient,
            ConfigurationMapper configurationMapper) {
        this.sapCpsClient = sapCpsClient;
        this.sapKbClient = sapKbClient;
        this.configurationMapper = configurationMapper;
    }

    public ConfigurationResponse createConfiguration(CreateConfigurationRequest request) {
        long start = System.currentTimeMillis();

        SapCreateRequest sapRequest = new SapCreateRequest();
        sapRequest.setProductKey(request.getProductId());

        if (request.getKbId() != null && !request.getKbId().isBlank()) {
            sapRequest.setKbId(Integer.valueOf(request.getKbId()));
        }

        sapRequest.setDate(java.time.LocalDate.now().toString());
        sapRequest.setContext(List.of(
                new SapCreateRequest.SapContextEntry("VBAP-VRKME", "EA")));
        sapRequest.setSource(
                new SapCreateRequest.SapSource("cpq", "quote_item", "10"));

        SapRuntimeConfigurationResponse runtimeResponse = sapCpsClient.createConfiguration(sapRequest);

        if (runtimeResponse == null) {
            throw new IllegalStateException("SAP CPS returned null body for createConfiguration");
        }

        if (runtimeResponse.getId() != null) {
            readOnlyByConfigurationId.put(runtimeResponse.getId(), false);
        }

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

        if (runtimeResponse.getId() != null && runtimeResult.getEtag() != null) {
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
            PatchConfigurationRequest request) {
        long start = System.currentTimeMillis();

        if (Boolean.TRUE.equals(readOnlyByConfigurationId.get(configId))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Configuration is read-only after snapshot restore");
        }

        // Bekommen Sie ETag — wenn nicht im Cache vorhanden, führen Sie einen GET-Vorgang durch
        String etag = etagByConfigurationId.get(configId);
        if (etag == null) {
            SapGetConfigurationResult runtimeResult = sapCpsClient.getConfigurationWithEtag(configId);
            if (runtimeResult.getBody() != null && runtimeResult.getEtag() != null) {
                etagByConfigurationId.put(configId, runtimeResult.getEtag());
                etag = runtimeResult.getEtag();
            }
        }

        //For root-level updates use itemId = "1" if not provided in the request
        String itemId = request.getItemId() != null && !request.getItemId().isBlank()
            ? request.getItemId()
            : "1";

        sapCpsClient.patchConfiguration(
                configId,
                itemId,
                request.getCharacteristicId(),
                request.getValue(),
                etag);

        // Nach PATCH — GET mit neuem ETag
        SapGetConfigurationResult refreshed = sapCpsClient.getConfigurationWithEtag(configId);
        SapRuntimeConfigurationResponse updatedRuntime = refreshed.getBody();

        if (refreshed.getEtag() != null) {
            etagByConfigurationId.put(configId, refreshed.getEtag());
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

        boolean liveRequested = request.getConfigurationId() != null && !request.getConfigurationId().isBlank();

        if (liveRequested) {
            try {
                ConfigurationResponse liveResponse = getConfiguration(request.getConfigurationId());

                RestoreInfo restoreInfo = new RestoreInfo();
                restoreInfo.setMode("resume");
                restoreInfo.setStatus("RESUMED");
                restoreInfo.setStrategy("LIVECONFIGURATION");
                restoreInfo.setLiveSessionAvailable(true);
                restoreInfo.setSnapshotUsed(false);
                restoreInfo.setReadOnly(false);
                restoreInfo.setMessage("Configuration restored from live CPS runtime");

                liveResponse.setRestoreInfo(restoreInfo);
                if (liveResponse.getConfigurationId() != null) {
                    readOnlyByConfigurationId.put(liveResponse.getConfigurationId(), false);
                }

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
            response.setGroups(snapshot.getGroups() != null ? snapshot.getGroups() : List.of());
            response.setMessages(snapshot.getMessages() != null ? snapshot.getMessages() : List.of());

            RestoreInfo restoreInfo = new RestoreInfo();
            restoreInfo.setMode("resume");
            restoreInfo.setStatus("FALLBACKAPPLIED");
            restoreInfo.setStrategy(liveRequested ? "SNAPSHOTFALLBACK" : "READONLYSNAPSHOT");
            restoreInfo.setLiveSessionAvailable(false);
            restoreInfo.setSnapshotUsed(true);
            restoreInfo.setReadOnly(true);
            restoreInfo.setMessage("Configuration restored from snapshot fallback");

            response.setRestoreInfo(restoreInfo);

            if (snapshot.getConfigurationId() != null && !snapshot.getConfigurationId().isBlank()) {
                readOnlyByConfigurationId.put(snapshot.getConfigurationId(), true);
            }

            response.setBackendProcessingTimeMs(System.currentTimeMillis() - start);
            return response;
        }

        throw new IllegalArgumentException("Resume requires configurationId or snapshot");
    }

    public ConfigurationResponse createFromExternalConfiguration(
        ExternalConfigurationCreateRequest request
    ) {
        long start = System.currentTimeMillis();

        log.info("createFromExternalConfiguration: request.kbId={}, request.productId={}", 
                request.getKbId(), request.getProductId());

        Map<String, Object> sapRequestBody = ExternalConfigurationMapper.toSapRequestBody(request);

        SapRuntimeConfigurationResponse createdRuntime =
                sapCpsClient.createConfigurationFromExternal(sapRequestBody);

        if (createdRuntime == null) {
            throw new IllegalStateException("SAP CPS returned null body for createConfigurationFromExternal");
        }

        String configurationId = createdRuntime.getId();
        log.info("SAP CPS external-create response: id={}, kbId={}, complete={}, consistent={}",
                configurationId, createdRuntime.getKbId(), createdRuntime.isComplete(), createdRuntime.isConsistent());

        if (configurationId == null || configurationId.isBlank()) {
            throw new IllegalStateException("SAP CPS external create response contains no configurationId");
        }

        SapGetConfigurationResult refreshed = sapCpsClient.getConfigurationWithEtag(configurationId);
        SapRuntimeConfigurationResponse refreshedRuntime = refreshed != null ? refreshed.getBody() : null;

        if (refreshedRuntime == null) {
            throw new IllegalStateException(
                    "SAP CPS returned null configuration body after external create for configId=" + configurationId
            );
        }

        if (refreshed != null && refreshed.getEtag() != null) {
            etagByConfigurationId.put(configurationId, refreshed.getEtag());
        }
        readOnlyByConfigurationId.put(configurationId, false);

        String kbId = refreshedRuntime.getKbId() != null
                ? refreshedRuntime.getKbId().toString()
                : request.getKbId();

        if ((kbId == null || kbId.isBlank()) && request.getProductId() != null && !request.getProductId().isBlank()) {
            log.warn("kbId not available, trying fallback via productId={}", request.getProductId());
            try {
                SapCreateRequest tempRequest = new SapCreateRequest();
                tempRequest.setProductKey(request.getProductId());
                SapRuntimeConfigurationResponse tempResponse = sapCpsClient.createConfiguration(tempRequest);

                if (tempResponse != null && tempResponse.getKbId() != null) {
                    kbId = tempResponse.getKbId().toString();
                    log.info("Resolved kbId from product: {}", kbId);

                    if (tempResponse.getId() != null) {
                        try {
                            sapCpsClient.deleteConfiguration(tempResponse.getId());
                            log.info("Deleted temporary configuration: {}", tempResponse.getId());
                        } catch (Exception cleanupEx) {
                            log.warn("Failed to cleanup temporary configuration: {}", tempResponse.getId(), cleanupEx);
                        }
                    }
                }
            } catch (Exception fallbackEx) {
                log.error("Failed to resolve kbId via productId", fallbackEx);
            }
        }

        log.info("Resolved kbId: {}", kbId);

        ConfigurationResponse response;
        if (kbId != null && !kbId.isBlank()) {
            SapKbResponse kbResponse = sapKbClient.getKnowledgeBase(kbId);
            response = configurationMapper.toWidgetResponse(refreshedRuntime, kbResponse);
        } else {
            response = configurationMapper.toWidgetResponse(refreshedRuntime, null);
        }

        RestoreInfo restoreInfo = new RestoreInfo();
        restoreInfo.setMode("create");
        restoreInfo.setStatus("RESUMED");
        restoreInfo.setStrategy("LIVECONFIGURATION");
        restoreInfo.setLiveSessionAvailable(true);
        restoreInfo.setSnapshotUsed(false);
        restoreInfo.setReadOnly(false);
        restoreInfo.setMessage("Configuration created from external snapshot");

        response.setRestoreInfo(restoreInfo);
        response.setBackendProcessingTimeMs(System.currentTimeMillis() - start);
        return response;
    }

    public ConfigurationResponse completeConfiguration(String configurationId) {
        long start = System.currentTimeMillis();

        if (Boolean.TRUE.equals(readOnlyByConfigurationId.get(configurationId))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Configuration is read-only after snapshot restore");
        }

        SapRuntimeConfigurationResponse completedRuntime = sapCpsClient.completeConfiguration(configurationId);

        if (completedRuntime == null) {
            throw new IllegalStateException(
                    "SAP CPS returned null body for completeConfiguration configurationId=" + configurationId);
        }

        if (!completedRuntime.isComplete() || !completedRuntime.isConsistent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Configuration is not complete or not consistent");
        }

        String kbId = completedRuntime.getKbId() != null
                ? completedRuntime.getKbId().toString()
                : null;

        SapKbResponse kbResponse = sapKbClient.getKnowledgeBase(kbId);

        ConfigurationResponse response = configurationMapper.toWidgetResponse(completedRuntime, kbResponse);
        response.setBackendProcessingTimeMs(System.currentTimeMillis() - start);
        return response;
    }

    public void deleteConfiguration(String configurationId) {
        if (configurationId == null || configurationId.isBlank()) {
            throw new IllegalArgumentException("configurationId must not be blank");
        }

        sapCpsClient.deleteConfiguration(configurationId);

        // Очистка кэша для удаленной конфигурации
        etagByConfigurationId.remove(configurationId);
        readOnlyByConfigurationId.remove(configurationId);
    }

    public DeleteConfigurationsResponse deleteConfigurations(DeleteConfigurationsRequest request) {
        List<String> configurationIds = request.getConfigurationIds();
        
        if (configurationIds == null || configurationIds.isEmpty()) {
            throw new IllegalArgumentException("configurationIds list must not be empty");
        }

        int totalRequested = configurationIds.size();
        int successfullyDeleted = 0;
        List<String> failedConfigurationIds = new ArrayList<>();

        for (String configId : configurationIds) {
            try {
                deleteConfiguration(configId);
                successfullyDeleted++;
            } catch (Exception ex) {
                failedConfigurationIds.add(configId);
            }
        }

        return new DeleteConfigurationsResponse(totalRequested, successfullyDeleted, failedConfigurationIds);
    }

}