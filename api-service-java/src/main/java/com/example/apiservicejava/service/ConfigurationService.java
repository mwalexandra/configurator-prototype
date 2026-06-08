package com.example.apiservicejava.service;

import com.example.apiservicejava.model.CreateConfigurationRequest;
import com.example.apiservicejava.model.CreateConfigurationResponse;
import com.example.apiservicejava.model.PatchConfigurationRequest;
import com.example.apiservicejava.model.PatchConfigurationResponse;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class ConfigurationService {

    private final SapCpsClient sapCpsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfigurationService(SapCpsClient sapCpsClient) {
        this.sapCpsClient = sapCpsClient;
    }

    public CreateConfigurationResponse createConfiguration(CreateConfigurationRequest request) {
        Instant start = Instant.now();

        String sapResponse = sapCpsClient.createConfiguration(
                request.getProductId(),
                request.getKbId(),
                request.getLocal()
        );

        // find the duration of request/response

        Instant end = Instant.now();
        long responseTimeMs = Duration.between(start, end).toMillis();

        try {
            JsonNode root = objectMapper.readTree(sapResponse);
            String configId = root.path("id").asText();

            return new CreateConfigurationResponse(
                configId,
                "STARTED",
                sapResponse,
                responseTimeMs
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse SAP configuration response", e);
        }
    }

    public PatchConfigurationResponse patchConfiguration(
            String configId,
            PatchConfigurationRequest request
    ) {
        return new PatchConfigurationResponse(configId, "UPDATED");
    }
}