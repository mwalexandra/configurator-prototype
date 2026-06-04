package com.example.apiservicejava.service;

import com.example.apiservicejava.model.CreateConfigurationRequest;
import com.example.apiservicejava.model.CreateConfigurationResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ConfigurationService {

    public CreateConfigurationResponse createConfiguration(CreateConfigurationRequest request) {
        String configId = UUID.randomUUID().toString();

        return new CreateConfigurationResponse(configId, "STARTED");
    }
}