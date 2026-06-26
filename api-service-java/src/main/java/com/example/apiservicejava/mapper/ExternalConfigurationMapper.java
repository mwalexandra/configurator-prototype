package com.example.apiservicejava.mapper;

import com.example.apiservicejava.model.api.ExternalConfigurationCharacteristicDto;
import com.example.apiservicejava.model.api.ExternalConfigurationCreateRequest;
import com.example.apiservicejava.model.api.ExternalConfigurationDto;
import com.example.apiservicejava.model.api.ExternalConfigurationItemDto;
import com.example.apiservicejava.model.api.ExternalConfigurationValueDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExternalConfigurationMapper {

    private ExternalConfigurationMapper() {
    }

    public static Map<String, Object> toSapRequestBody(ExternalConfigurationCreateRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("product", request.getProductId());
        body.put("kbId", request.getKbId());

        if (request.getExternalConfiguration() != null) {
            body.put("externalConfiguration", mapExternalConfiguration(request.getExternalConfiguration()));
        }

        return body;
    }

    private static Map<String, Object> mapExternalConfiguration(ExternalConfigurationDto dto) {
        Map<String, Object> result = new HashMap<>();

        if (dto.getRootItem() != null) {
            result.put("rootItem", mapItem(dto.getRootItem()));
        }

        if (dto.getMetadata() != null) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("version", dto.getMetadata().getVersion());
            metadata.put("sourceContext", dto.getMetadata().getSourceContext());
            metadata.put("configurationId", dto.getMetadata().getConfigurationId());
            metadata.put("savedAt", dto.getMetadata().getSavedAt());
            result.put("metadata", metadata);
        }

        return result;
    }

    private static Map<String, Object> mapItem(ExternalConfigurationItemDto item) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", item.getId());
        result.put("key", item.getKey());

        if (item.getCharacteristics() != null && !item.getCharacteristics().isEmpty()) {
            List<Map<String, Object>> characteristics = new ArrayList<>();
            for (ExternalConfigurationCharacteristicDto characteristic : item.getCharacteristics()) {
                characteristics.add(mapCharacteristic(characteristic));
            }
            result.put("characteristics", characteristics);
        }

        if (item.getSubItems() != null && !item.getSubItems().isEmpty()) {
            List<Map<String, Object>> subItems = new ArrayList<>();
            for (ExternalConfigurationItemDto subItem : item.getSubItems()) {
                subItems.add(mapItem(subItem));
            }
            result.put("subItems", subItems);
        }

        return result;
    }

    private static Map<String, Object> mapCharacteristic(ExternalConfigurationCharacteristicDto characteristic) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", characteristic.getId());

        if (characteristic.getValues() != null && !characteristic.getValues().isEmpty()) {
            List<Map<String, Object>> values = new ArrayList<>();
            for (ExternalConfigurationValueDto value : characteristic.getValues()) {
                values.add(mapValue(value));
            }
            result.put("values", values);
        }

        return result;
    }

    private static Map<String, Object> mapValue(ExternalConfigurationValueDto value) {
        Map<String, Object> result = new HashMap<>();
        result.put("value", value.getValue());
        return result;
    }
}