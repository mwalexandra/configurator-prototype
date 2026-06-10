package com.example.apiservicejava.controller;

import com.example.apiservicejava.model.CreateConfigurationRequest;
import com.example.apiservicejava.model.ConfigurationResponse;
import com.example.apiservicejava.model.PatchConfigurationRequest;
import com.example.apiservicejava.service.ConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/configurations")
@CrossOrigin(origins = "*")
public class ConfigurationController {

    private final ConfigurationService configurationService;

    public ConfigurationController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConfigurationResponse createConfiguration(@RequestBody CreateConfigurationRequest request) {
        return configurationService.createConfiguration(request);
    }

    @GetMapping("/{configId}")
    @ResponseStatus(HttpStatus.OK)
    public ConfigurationResponse getConfiguration(@PathVariable String configId) {
        return configurationService.getConfiguration(configId);
    }

    @PatchMapping("/{configId}")
    @ResponseStatus(HttpStatus.OK)
    public ConfigurationResponse patchConfiguration(
            @PathVariable String configId,
            @RequestBody PatchConfigurationRequest request
    ) {
        return configurationService.patchConfiguration(configId, request);
    }
}