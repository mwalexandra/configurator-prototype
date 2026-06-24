package com.example.apiservicejava.service;

import com.example.apiservicejava.model.api.SaveConfigurationRequest;
import com.example.apiservicejava.model.api.SavedConfigurationDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SavedConfigurationService {

    private final ConfigurationFileRepository fileRepository;

    public SavedConfigurationService(ConfigurationFileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public List<SavedConfigurationDto> list() throws IOException {
        List<SavedConfigurationDto> result = new ArrayList<>();

        for (String fileName : fileRepository.listJsonFiles()) {
            SavedConfigurationDto dto = fileRepository.read(fileName, SavedConfigurationDto.class);
            dto.setFileName(fileName);
            result.add(dto);
        }

        return result;
    }

    public SavedConfigurationDto get(String fileName) throws IOException {
        SavedConfigurationDto dto = fileRepository.read(fileName, SavedConfigurationDto.class);
        dto.setFileName(fileName);
        return dto;
    }

    public SavedConfigurationDto save(SaveConfigurationRequest request) throws IOException {
        SavedConfigurationDto dto = new SavedConfigurationDto();

        String id = request.getId();
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }

        dto.setId(id);
        dto.setLabel(request.getLabel());
        dto.setProductId(request.getProductId());
        dto.setConfigurationId(request.getConfigurationId());
        dto.setSavedAt(OffsetDateTime.now());
        dto.setSnapshot(request.getSnapshot());

        String fileName = id + ".json";
        dto.setFileName(fileName);

        fileRepository.save(fileName, dto);
        return dto;
    }
}