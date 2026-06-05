package com.example.apiservicejava.service;

import com.example.apiservicejava.model.CreateConfigurationRequest;
import com.example.apiservicejava.model.CreateConfigurationResponse;
import com.example.apiservicejava.model.PatchConfigurationRequest;
import com.example.apiservicejava.model.PatchConfigurationResponse;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ConfigurationService {

    public CreateConfigurationResponse createConfiguration(CreateConfigurationRequest request) {
        String configId = UUID.randomUUID().toString();

        return new CreateConfigurationResponse(configId, "STARTED");
    }

    public PatchConfigurationResponse patchConfiguration(
            String configId,
            PatchConfigurationRequest request
    ) {
        return new PatchConfigurationResponse(configId, "UPDATED");
    }
}