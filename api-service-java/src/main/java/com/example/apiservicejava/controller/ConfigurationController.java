package com.example.apiservicejava.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.apiservicejava.model.api.ConfigurationResponse;
import com.example.apiservicejava.model.api.CreateConfigurationRequest;
import com.example.apiservicejava.model.api.DeleteConfigurationsRequest;
import com.example.apiservicejava.model.api.DeleteConfigurationsResponse;
import com.example.apiservicejava.model.api.ExternalConfigurationCreateRequest;
import com.example.apiservicejava.model.api.PatchConfigurationRequest;
import com.example.apiservicejava.model.api.ResumeConfigurationRequest;
import com.example.apiservicejava.service.ConfigurationService;

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

    @PostMapping("/external")
    public ResponseEntity<ConfigurationResponse> createFromExternalConfiguration(
        @RequestBody ExternalConfigurationCreateRequest request
    ) {
        ConfigurationResponse response = configurationService.createFromExternalConfiguration(request);
        return ResponseEntity.ok(response);
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

    @DeleteMapping("/{configId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConfiguration(@PathVariable String configId) {
        configurationService.deleteConfiguration(configId);
    }

    @PostMapping("/batch/delete")
    @ResponseStatus(HttpStatus.OK)
    public DeleteConfigurationsResponse deleteConfigurations(@Valid @RequestBody DeleteConfigurationsRequest request) {
        return configurationService.deleteConfigurations(request);
    }
}