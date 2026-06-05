package com.example.apiservicejava.controller;

import com.example.apiservicejava.model.CreateConfigurationRequest;
import com.example.apiservicejava.model.CreateConfigurationResponse;
import com.example.apiservicejava.model.PatchConfigurationRequest;
import com.example.apiservicejava.model.PatchConfigurationResponse;

import com.example.apiservicejava.service.ConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/configurations")
public class ConfigurationController {

    private final ConfigurationService configurationService;

    public ConfigurationController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @PostMapping                                               // POST endpoint
    @ResponseStatus(HttpStatus.CREATED)                        // 201 Created
    public CreateConfigurationResponse createConfiguration(
            @RequestBody                                       // Deserialization from JSON to Java-Model
            CreateConfigurationRequest request
    ) {
        return configurationService.createConfiguration(request);
    }

    @PatchMapping("/{configId}")                                // PATCH endpoint
    @ResponseStatus(HttpStatus.OK)
    public PatchConfigurationResponse patchConfiguration(
            @PathVariable String configId,
            @RequestBody PatchConfigurationRequest request
    ) {
        return configurationService.patchConfiguration(configId, request);
    }
}