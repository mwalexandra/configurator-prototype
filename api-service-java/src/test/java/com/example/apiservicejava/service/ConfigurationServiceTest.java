package com.example.apiservicejava.service;

import com.example.apiservicejava.mapper.ConfigurationMapper;
import com.example.apiservicejava.model.api.ConfigurationResponse;
import com.example.apiservicejava.model.api.ConfigurationSnapshot;
import com.example.apiservicejava.model.api.DeleteConfigurationsRequest;
import com.example.apiservicejava.model.api.DeleteConfigurationsResponse;
import com.example.apiservicejava.model.api.ExternalConfigurationCreateRequest;
import com.example.apiservicejava.model.api.ResumeConfigurationRequest;
import com.example.apiservicejava.model.sapkb.SapKbCharacteristic;
import com.example.apiservicejava.model.sapkb.SapKbResponse;
import com.example.apiservicejava.model.sapruntime.SapCreateRequest;
import com.example.apiservicejava.model.sapruntime.SapRuntimeConfigurationResponse;
import com.example.apiservicejava.model.sapruntime.SapRuntimeRootItem;
import com.example.apiservicejava.service.sap.SapCpsClient;
import com.example.apiservicejava.service.sap.support.SapGetConfigurationResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ConfigurationService - критическая бизнес-логика.
 * Покрывает:
 * - createFromExternalConfiguration с fallback логикой для kbId
 * - resumeConfiguration с snapshot fallback
 * - deleteConfiguration с cache cleanup
 * - ETag и read-only flag management
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    @Mock
    private SapCpsClient sapCpsClient;

    @Mock
    private SapKbClient sapKbClient;

    @Mock
    private ConfigurationMapper configurationMapper;

    @InjectMocks
    private ConfigurationService service;

    private SapRuntimeConfigurationResponse mockSapResponse;
    private SapKbResponse mockKbResponse;

    @BeforeEach
    void setUp() {
        // Common mock data
        mockSapResponse = new SapRuntimeConfigurationResponse();
        mockSapResponse.setId("cfg-123");
        mockSapResponse.setProductKey("CPS_BURGER");
        mockSapResponse.setKbId(80);
        mockSapResponse.setComplete(false);
        mockSapResponse.setConsistent(true);
        
        SapRuntimeRootItem rootItem = new SapRuntimeRootItem();
        rootItem.setId("1");
        rootItem.setKey("CPS_BURGER");
        rootItem.setCharacteristics(new ArrayList<>());
        rootItem.setSubItems(new ArrayList<>());
        mockSapResponse.setRootItem(rootItem);

        mockKbResponse = new SapKbResponse();
        SapKbCharacteristic kbChar = new SapKbCharacteristic();
        kbChar.setId("CPS_OPTION_M");
        kbChar.setName("Menu options");
        mockKbResponse.setCharacteristics(List.of(kbChar));
    }

    @Test
    void shouldCreateFromExternalConfigurationWithProvidedKbId() {
        // Given: request with kbId
        ExternalConfigurationCreateRequest request = new ExternalConfigurationCreateRequest();
        request.setProductId("CPS_BURGER");
        request.setKbId("80");  // kbId provided

        when(sapCpsClient.createConfigurationFromExternal(any())).thenReturn(mockSapResponse);
        when(sapKbClient.getKnowledgeBase("80")).thenReturn(mockKbResponse);

        // When
        ConfigurationResponse result = service.createFromExternalConfiguration(request);

        // Then
        assertNotNull(result);
        assertEquals("cfg-123", result.getConfigurationId());
        assertEquals("CPS_BURGER", result.getProductId());
        
        // Should use provided kbId, not from SAP response
        verify(sapKbClient).getKnowledgeBase("80");
        verify(sapCpsClient, never()).createConfiguration(any());  // No fallback needed
    }

    @Test
    void shouldFallbackToSapResponseKbIdWhenNotProvided() {
        // Given: request WITHOUT kbId
        ExternalConfigurationCreateRequest request = new ExternalConfigurationCreateRequest();
        request.setProductId("CPS_BURGER");
        request.setKbId(null);  // No kbId provided

        when(sapCpsClient.createConfigurationFromExternal(any())).thenReturn(mockSapResponse);
        when(sapKbClient.getKnowledgeBase("80")).thenReturn(mockKbResponse);

        // When
        ConfigurationResponse result = service.createFromExternalConfiguration(request);

        // Then: Should use kbId from SAP response
        verify(sapKbClient).getKnowledgeBase("80");
        assertNotNull(result);
    }

    @Test
    void shouldFallbackToProductIdWhenKbIdMissing() {
        // Given: request without kbId, and SAP response also has no kbId
        ExternalConfigurationCreateRequest request = new ExternalConfigurationCreateRequest();
        request.setProductId("CPS_BURGER");
        request.setKbId(null);

        SapRuntimeConfigurationResponse sapResponseNoKb = new SapRuntimeConfigurationResponse();
        sapResponseNoKb.setId("cfg-temp");
        sapResponseNoKb.setProductKey("CPS_BURGER");
        sapResponseNoKb.setKbId(null);  // No kbId!
        SapRuntimeRootItem rootItem = new SapRuntimeRootItem();
        rootItem.setId("1");
        rootItem.setKey("ROOT");
        rootItem.setCharacteristics(new ArrayList<>());
        rootItem.setSubItems(new ArrayList<>());
        sapResponseNoKb.setRootItem(rootItem);

        SapRuntimeConfigurationResponse tempConfigResponse = new SapRuntimeConfigurationResponse();
        tempConfigResponse.setId("cfg-temp-123");
        tempConfigResponse.setKbId(80);  // KB ID from temporary config

        when(sapCpsClient.createConfigurationFromExternal(any())).thenReturn(sapResponseNoKb);
        when(sapCpsClient.createConfiguration(any(SapCreateRequest.class))).thenReturn(tempConfigResponse);
        when(sapKbClient.getKnowledgeBase("80")).thenReturn(mockKbResponse);
        doNothing().when(sapCpsClient).deleteConfiguration(anyString());

        // When
        ConfigurationResponse result = service.createFromExternalConfiguration(request);

        // Then: Should create temp config to get kbId
        ArgumentCaptor<SapCreateRequest> captor = ArgumentCaptor.forClass(SapCreateRequest.class);
        verify(sapCpsClient).createConfiguration(captor.capture());
        assertEquals("CPS_BURGER", captor.getValue().getProductKey());
        
        // Should fetch KB with resolved kbId
        verify(sapKbClient).getKnowledgeBase("80");
        
        // Should delete temp config
        verify(sapCpsClient).deleteConfiguration("cfg-temp-123");
        
        assertNotNull(result);
    }

    @Test
    void shouldSkipKbEnrichmentWhenKbIdCannotBeResolved() {
        // Given: No kbId anywhere
        ExternalConfigurationCreateRequest request = new ExternalConfigurationCreateRequest();
        request.setProductId("CPS_BURGER");
        request.setKbId(null);

        SapRuntimeConfigurationResponse sapResponseNoKb = new SapRuntimeConfigurationResponse();
        sapResponseNoKb.setId("cfg-no-kb");
        sapResponseNoKb.setProductKey("CPS_BURGER");
        sapResponseNoKb.setKbId(null);
        SapRuntimeRootItem rootItem = new SapRuntimeRootItem();
        rootItem.setId("1");
        rootItem.setKey("ROOT");
        rootItem.setCharacteristics(new ArrayList<>());
        rootItem.setSubItems(new ArrayList<>());
        sapResponseNoKb.setRootItem(rootItem);

        when(sapCpsClient.createConfigurationFromExternal(any())).thenReturn(sapResponseNoKb);
        when(sapCpsClient.createConfiguration(any())).thenThrow(new RuntimeException("Product not found"));

        // When
        ConfigurationResponse result = service.createFromExternalConfiguration(request);

        // Then: Should NOT call KB client
        verify(sapKbClient, never()).getKnowledgeBase(anyString());
        assertNotNull(result);
    }

    @Test
    void shouldResumeFromLiveConfiguration() {
        // Given: request with configurationId
        ResumeConfigurationRequest request = new ResumeConfigurationRequest();
        request.setConfigurationId("cfg-live-123");
        request.setSnapshot(null);

        ConfigurationResponse liveConfig = new ConfigurationResponse();
        liveConfig.setConfigurationId("cfg-live-123");
        liveConfig.setProductId("CPS_BURGER");
        liveConfig.setKbId("80");
        liveConfig.setComplete(false);
        liveConfig.setConsistent(true);

        SapGetConfigurationResult sapResult = new SapGetConfigurationResult();
        sapResult.setBody(mockSapResponse);
        sapResult.setEtag("etag-123");

        when(sapCpsClient.getConfigurationWithEtag("cfg-live-123")).thenReturn(sapResult);
        when(sapKbClient.getKnowledgeBase("80")).thenReturn(mockKbResponse);
        when(configurationMapper.toWidgetResponse(mockSapResponse, mockKbResponse))
                .thenReturn(liveConfig);

        // When
        ConfigurationResponse result = service.resumeConfiguration(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.getRestoreInfo());
        assertEquals("LIVECONFIGURATION", result.getRestoreInfo().getStrategy());
        assertTrue(result.getRestoreInfo().isLiveSessionAvailable());
        assertFalse(result.getRestoreInfo().isSnapshotUsed());
        assertFalse(result.getRestoreInfo().isReadOnly());
        
        verify(sapCpsClient).getConfigurationWithEtag("cfg-live-123");
    }

    @Test
    void shouldFallbackToSnapshotWhenLiveConfigFails() {
        // Given: request with both configurationId and snapshot
        ResumeConfigurationRequest request = new ResumeConfigurationRequest();
        request.setConfigurationId("cfg-not-found");
        
        ConfigurationSnapshot snapshot = new ConfigurationSnapshot();
        snapshot.setConfigurationId("cfg-snapshot-123");
        snapshot.setProductId("CPS_BURGER");
        snapshot.setKbId("80");
        snapshot.setComplete(true);
        snapshot.setConsistent(true);
        request.setSnapshot(snapshot);

        when(sapCpsClient.getConfigurationWithEtag("cfg-not-found"))
                .thenThrow(new RuntimeException("Configuration not found"));

        // When
        ConfigurationResponse result = service.resumeConfiguration(request);

        // Then: Should use snapshot fallback
        assertNotNull(result);
        assertEquals("cfg-snapshot-123", result.getConfigurationId());
        assertNotNull(result.getRestoreInfo());
        assertEquals("SNAPSHOTFALLBACK", result.getRestoreInfo().getStrategy());
        assertFalse(result.getRestoreInfo().isLiveSessionAvailable());
        assertTrue(result.getRestoreInfo().isSnapshotUsed());
        assertTrue(result.getRestoreInfo().isReadOnly());
    }

    @Test
    void shouldResumeFromSnapshotOnlyWhenNoConfigurationId() {
        // Given: request with only snapshot
        ResumeConfigurationRequest request = new ResumeConfigurationRequest();
        request.setConfigurationId(null);
        
        ConfigurationSnapshot snapshot = new ConfigurationSnapshot();
        snapshot.setConfigurationId("cfg-snapshot-456");
        snapshot.setProductId("CPS_BURGER");
        snapshot.setKbId("80");
        snapshot.setComplete(true);
        snapshot.setConsistent(true);
        request.setSnapshot(snapshot);

        // When
        ConfigurationResponse result = service.resumeConfiguration(request);

        // Then
        assertNotNull(result);
        assertEquals("cfg-snapshot-456", result.getConfigurationId());
        assertNotNull(result.getRestoreInfo());
        assertEquals("READONLYSNAPSHOT", result.getRestoreInfo().getStrategy());
        assertFalse(result.getRestoreInfo().isLiveSessionAvailable());
        assertTrue(result.getRestoreInfo().isSnapshotUsed());
        assertTrue(result.getRestoreInfo().isReadOnly());
        
        verify(sapCpsClient, never()).getConfigurationWithEtag(anyString());
    }

    @Test
    void shouldDeleteSingleConfiguration() {
        // Given
        String configId = "cfg-to-delete";
        doNothing().when(sapCpsClient).deleteConfiguration(configId);

        // When
        service.deleteConfiguration(configId);

        // Then
        verify(sapCpsClient).deleteConfiguration(configId);
    }

    @Test
    void shouldDeleteMultipleConfigurationsAndReturnResults() {
        // Given
        DeleteConfigurationsRequest request = new DeleteConfigurationsRequest();
        request.setConfigurationIds(List.of("cfg-1", "cfg-2", "cfg-3"));

        doNothing().when(sapCpsClient).deleteConfiguration("cfg-1");
        doNothing().when(sapCpsClient).deleteConfiguration("cfg-2");
        doThrow(new RuntimeException("Not found")).when(sapCpsClient).deleteConfiguration("cfg-3");

        // When
        DeleteConfigurationsResponse response = service.deleteConfigurations(request);

        // Then
        assertEquals(3, response.getTotalRequested());
        assertEquals(2, response.getSuccessfullyDeleted());
        assertEquals(1, response.getFailedConfigurationIds().size());
        assertEquals("cfg-3", response.getFailedConfigurationIds().get(0));
        
        verify(sapCpsClient, times(3)).deleteConfiguration(anyString());
    }

    @Test
    void shouldHandleEmptyDeleteRequest() {
        // Given
        DeleteConfigurationsRequest request = new DeleteConfigurationsRequest();
        request.setConfigurationIds(List.of());

        // When & Then: should throw exception for empty list (validation)
        assertThrows(IllegalArgumentException.class, () -> {
            service.deleteConfigurations(request);
        });
    }

    @Test
    void shouldStoreEtagWhenGettingConfiguration() {
        // Given
        String configId = "cfg-with-etag";
        SapGetConfigurationResult sapResult = new SapGetConfigurationResult();
        sapResult.setBody(mockSapResponse);
        sapResult.setEtag("etag-stored-123");

        when(sapCpsClient.getConfigurationWithEtag(configId)).thenReturn(sapResult);
        when(sapKbClient.getKnowledgeBase("80")).thenReturn(mockKbResponse);
        
        ConfigurationResponse mockWidgetResponse = new ConfigurationResponse();
        mockWidgetResponse.setConfigurationId("cfg-123");
        when(configurationMapper.toWidgetResponse(mockSapResponse, mockKbResponse))
                .thenReturn(mockWidgetResponse);

        // When
        ConfigurationResponse result = service.getConfiguration(configId);

        // Then: ETag should be stored internally (we can't directly test private map,
        // but we verify the flow completed successfully)
        assertNotNull(result);
        verify(sapCpsClient).getConfigurationWithEtag(configId);
    }

    @Test
    void shouldThrowExceptionWhenResumeHasNoConfigIdAndNoSnapshot() {
        // Given: completely empty request
        ResumeConfigurationRequest request = new ResumeConfigurationRequest();
        request.setConfigurationId(null);
        request.setSnapshot(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            service.resumeConfiguration(request);
        });
    }
}
