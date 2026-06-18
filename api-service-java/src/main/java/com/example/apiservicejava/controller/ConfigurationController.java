package com.example.apiservicejava.controller;

import com.example.apiservicejava.model.api.CreateConfigurationRequest;
import com.example.apiservicejava.model.api.ConfigurationResponse;
import com.example.apiservicejava.model.api.PatchConfigurationRequest;
import com.example.apiservicejava.model.api.ResumeConfigurationRequest;
import com.example.apiservicejava.service.ConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("api/configurations")
// @CrossOrigin(origins = "*")
public class ConfigurationController {

    private final ConfigurationService configurationService;

    public ConfigurationController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConfigurationResponse createConfiguration(@Valid @RequestBody CreateConfigurationRequest request) {
        return configurationService.createConfiguration(request);
    }

    @PostMapping("/resume")
    @ResponseStatus(HttpStatus.OK)
    public ConfigurationResponse resumeConfiguration(@RequestBody ResumeConfigurationRequest request) {
        return configurationService.resumeConfiguration(request);
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

    @PostMapping("/{configId}/complete")
    @ResponseStatus(HttpStatus.OK)
    public ConfigurationResponse completeConfiguration(@PathVariable String configId) {
        return configurationService.completeConfiguration(configId);
    }
}