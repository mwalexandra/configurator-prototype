package com.example.apiservicejava.controller;

import com.example.apiservicejava.model.api.SaveConfigurationRequest;
import com.example.apiservicejava.model.api.SavedConfigurationDto;
import com.example.apiservicejava.service.SavedConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class SavedConfigurationController {

    private final SavedConfigurationService savedConfigurationService;

    public SavedConfigurationController(SavedConfigurationService savedConfigurationService) {
        this.savedConfigurationService = savedConfigurationService;
    }

    @GetMapping("/api/saved-configurations")
    public List<SavedConfigurationDto> list() throws IOException {
        return savedConfigurationService.list();
    }

    @GetMapping("/api/saved-configurations/{fileName}")
    public SavedConfigurationDto get(@PathVariable String fileName) throws IOException {
        return savedConfigurationService.get(fileName);
    }

    @PostMapping("/api/saved-configurations")
    @ResponseStatus(HttpStatus.CREATED)
    public SavedConfigurationDto save(@RequestBody SaveConfigurationRequest request) throws IOException {
        return savedConfigurationService.save(request);
    }
}