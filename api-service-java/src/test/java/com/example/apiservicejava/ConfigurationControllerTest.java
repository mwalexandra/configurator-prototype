package com.example.apiservicejava;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.apiservicejava.controller.ConfigurationController;
import com.example.apiservicejava.model.api.ConfigurationResponse;
import com.example.apiservicejava.model.api.CreateConfigurationRequest;
import com.example.apiservicejava.model.api.PatchConfigurationRequest;
import com.example.apiservicejava.service.ConfigurationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

// Testet nur den Web-Layer (Controller) für ConfigurationController
@WebMvcTest(ConfigurationController.class)
class ConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // für JSON (Request/Response)

    @MockitoBean
    // Service wird gemockt, damit im Test kein echter CPS-Aufruf stattfindet
    private ConfigurationService configurationService;

    @Test
    void shouldCreateConfigurationAndReturnResponse() throws Exception {
        // Arrange: Request-DTO und erwartete Response vorbereiten
        CreateConfigurationRequest request = new CreateConfigurationRequest();
        request.setProductId("CPS_BURGER");
        request.setKbId("80");

        ConfigurationResponse response = new ConfigurationResponse();
        response.setConfigurationId("cfg-123");
        response.setProductId("CPS_BURGER");
        response.setKbId("80");
        response.setComplete(false);
        response.setConsistent(true);

        // Service-Mock: wenn createConfiguration aufgerufen wird, Response zurückgeben
        when(configurationService.createConfiguration(any(CreateConfigurationRequest.class))).thenReturn(response);

        // Act + Assert: POST /api/configurations aufrufen und Response prüfen
        mockMvc.perform(post("/api/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // HTTP-Status prüfen
                .andExpect(status().isCreated())
                // Content-Type prüfen
                //.andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // JSON-Felder prüfen
                .andExpect(jsonPath("$.configurationId").value("cfg-123"))
                .andExpect(jsonPath("$.productId").value("CPS_BURGER"))
                .andExpect(jsonPath("$.kbId").value("80"))
                .andExpect(jsonPath("$.complete").value(false));
    }

    @Test
    void shouldReturnBadRequestOnInvalidCreateRequest() throws Exception {
        // Arrange: bewusst unvollständiger Request (ohne productId/kbId)
        CreateConfigurationRequest invalidRequest = new CreateConfigurationRequest();
        // keine Felder gesetzt

        // Act + Assert: JSON schicken и ожидать 400 (например, из-за @Valid)
        mockMvc.perform(post("/api/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        // Hinweis: dafür ist @Valid + Bean Validation im Controller/DTO notwendig, sonst wird 200 zurückgegeben.
    }

    @Test
    void shouldUpdateCharacteristicAndReturnUpdatedConfig() throws Exception {
        // Arrange
        String configId = "cfg-123";

        PatchConfigurationRequest request = new PatchConfigurationRequest();
        request.setCharacteristicId("CPS_OPTION_M");
        request.setValue("M");

        ConfigurationResponse updated = new ConfigurationResponse();
        updated.setConfigurationId(configId);
        updated.setProductId("CPS_BURGER");
        updated.setKbId("80");
        updated.setComplete(true);
        updated.setConsistent(true);

        when(configurationService.patchConfiguration(eq(configId), any(PatchConfigurationRequest.class))).thenReturn(updated);

        // Act + Assert: PATCH /api/configurations/{id}
        mockMvc.perform(patch("/api/configurations/{configId}", configId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurationId").value("cfg-123"))
                .andExpect(jsonPath("$.complete").value(true));
    }

    @Test
    void shouldCompleteConfigurationAndReturnFinalState() throws Exception {
        // Arrange
        String configId = "cfg-123";

        ConfigurationResponse completed = new ConfigurationResponse();
        completed.setConfigurationId(configId);
        completed.setProductId("CPS_BURGER");
        completed.setKbId("80");
        completed.setComplete(true);
        completed.setConsistent(true);

        when(configurationService.completeConfiguration(configId)).thenReturn(completed);

        // Act + Assert: POST /api/configurations/{id}/complete
        mockMvc.perform(post("/api/configurations/{configId}/complete", configId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurationId").value("cfg-123"))
                .andExpect(jsonPath("$.complete").value(true));
    }
}