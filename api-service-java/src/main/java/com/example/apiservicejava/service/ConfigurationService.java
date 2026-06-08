package com.example.apiservicejava.service;

import com.example.apiservicejava.model.CreateConfigurationRequest;
import com.example.apiservicejava.model.CreateConfigurationResponse;
import com.example.apiservicejava.model.PatchConfigurationRequest;
import com.example.apiservicejava.model.PatchConfigurationResponse;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ConfigurationService {

    private final SapCpsClient sapCpsClient;

    public ConfigurationService(SapCpsClient sapCpsClient){
        this.sapCpsClient = sapCpsClient;
    }

    public CreateConfigurationResponse createConfiguration(CreateConfigurationRequest request) {
        String sapResponse = sapCpsClient.createConfiguration(
            request.getProductId(),
            request.getKbId(),
            request.getLocal()
        );

        return new CreateConfigurationResponse(sapResponse, "STARTED");
    }

    public PatchConfigurationResponse patchConfiguration(
        String configId,
        PatchConfigurationRequest request
    ) {
        return new PatchConfigurationResponse(configId, "UPDATED");
    }
}