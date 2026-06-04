package com.example.apiservicejava.controller;

import com.example.apiservicejava.model.CreateConfigurationRequest;
import com.example.apiservicejava.model.CreateConfigurationResponse;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateConfigurationResponse createConfiguration(
            @RequestBody 
            CreateConfigurationRequest request
    ) {
        return configurationService.createConfiguration(request);
    }
}