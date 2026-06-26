package com.example.apiservicejava.mapper;

import com.example.apiservicejava.model.api.CharacteristicDto;
import com.example.apiservicejava.model.api.CharacteristicValueDto;
import com.example.apiservicejava.model.api.ConfigurationItem;
import com.example.apiservicejava.model.api.ConfigurationResponse;
import com.example.apiservicejava.model.api.ExternalConfigurationCharacteristicDto;
import com.example.apiservicejava.model.api.ExternalConfigurationCreateRequest;
import com.example.apiservicejava.model.api.ExternalConfigurationDto;
import com.example.apiservicejava.model.api.ExternalConfigurationItemDto;
import com.example.apiservicejava.model.api.ExternalConfigurationValueDto;
import com.example.apiservicejava.model.sapruntime.SapRuntimeCharacteristic;
import com.example.apiservicejava.model.sapruntime.SapRuntimeConfigurationResponse;
import com.example.apiservicejava.model.sapruntime.SapRuntimePossibleValue;
import com.example.apiservicejava.model.sapruntime.SapRuntimeRootItem;
import com.example.apiservicejava.model.sapruntime.SapRuntimeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExternalConfigurationMapper {

    private ExternalConfigurationMapper() {
    }

    public static Map<String, Object> toSapRequestBody(ExternalConfigurationCreateRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("kbId", parseInteger(request.getKbId()));

        if (request.getExternalConfiguration() != null) {
            mapExternalConfigurationToRoot(body, request.getExternalConfiguration(), request.getProductId());
        }

        return body;
    }

    private static void mapExternalConfigurationToRoot(
            Map<String, Object> body,
            ExternalConfigurationDto dto,
            String productId
    ) {
        if (dto == null) {
            return;
        }

        body.put("configurationDate", "2018-08-09");
        body.put("complete", false);
        body.put("consistent", true);
        body.put("locked", false);

        Map<String, Object> source = new HashMap<>();
        source.put("application", "generic");
        source.put("type", "product");
        source.put("id", productId != null ? productId : "CPS_BURGER");
        body.put("source", source);

        if (dto.getRootItem() != null) {
            body.put("rootItem", mapItem(dto.getRootItem(), productId, true));
        }
    }

    private static Map<String, Object> mapItem(
            ExternalConfigurationItemDto item,
            String productId,
            boolean root
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", item.getId());
        result.put("complete", false);
        result.put("consistent", true);
        result.put("salesRelevant", false);
        result.put("variantConditions", new ArrayList<>());

        if (root) {
            Map<String, Object> objectKey = new HashMap<>();
            objectKey.put("id", productId != null ? productId : item.getKey());
            objectKey.put("type", "MARA");
            objectKey.put("classType", "300");
            result.put("objectKey", objectKey);
            result.put("objectKeyAuthor", "5");

            Map<String, Object> quantity = new HashMap<>();
            quantity.put("value", 1);
            quantity.put("unit", "PCE");
            result.put("quantity", quantity);
            result.put("fixedQuantity", false);
        } else {
            Map<String, Object> objectKey = new HashMap<>();
            objectKey.put("id", item.getKey() != null ? item.getKey() : "SUB_ITEM");
            objectKey.put("type", "MARA");
            objectKey.put("classType", "300");
            result.put("objectKey", objectKey);
            result.put("objectKeyAuthor", "5");

            result.put("bomPosition", item.getId());
            result.put("bomPositionAuthor", "2");

            Map<String, Object> bomPositionObjectKey = new HashMap<>();
            bomPositionObjectKey.put("id", productId != null ? productId : "CPS_BURGER");
            bomPositionObjectKey.put("type", "MARA");
            bomPositionObjectKey.put("classType", "300");
            result.put("bomPositionObjectKey", bomPositionObjectKey);

            Map<String, Object> quantity = new HashMap<>();
            quantity.put("value", 1);
            quantity.put("unit", "PCE");
            result.put("quantity", quantity);
            result.put("fixedQuantity", false);
        }

        if (item.getCharacteristics() != null && !item.getCharacteristics().isEmpty()) {
            List<Map<String, Object>> characteristics = new ArrayList<>();
            for (ExternalConfigurationCharacteristicDto characteristic : item.getCharacteristics()) {
                characteristics.add(mapCharacteristic(characteristic));
            }
            result.put("characteristics", characteristics);
        } else {
            result.put("characteristics", new ArrayList<>());
        }

        if (item.getSubItems() != null && !item.getSubItems().isEmpty()) {
            List<Map<String, Object>> subItems = new ArrayList<>();
            for (ExternalConfigurationItemDto subItem : item.getSubItems()) {
                subItems.add(mapItem(subItem, productId, false));
            }
            result.put("subItems", subItems);
        } else {
            result.put("subItems", new ArrayList<>());
        }

        return result;
    }

    private static Map<String, Object> mapCharacteristic(ExternalConfigurationCharacteristicDto characteristic) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", characteristic.getId());
        result.put("required", false);
        result.put("visible", true);

        if (characteristic.getValues() != null && !characteristic.getValues().isEmpty()) {
            List<Map<String, Object>> values = new ArrayList<>();
            for (ExternalConfigurationValueDto value : characteristic.getValues()) {
                values.add(mapValue(value));
            }
            result.put("values", values);
        } else {
            result.put("values", new ArrayList<>());
        }

        return result;
    }

    private static Map<String, Object> mapValue(ExternalConfigurationValueDto value) {
        Map<String, Object> result = new HashMap<>();
        result.put("value", value.getValue());
        result.put("author", "8");
        return result;
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.valueOf(value);
    }

    public static ConfigurationResponse fromSapRuntimeResponse(SapRuntimeConfigurationResponse sapResponse) {
        if (sapResponse == null) {
            return null;
        }

        ConfigurationResponse response = new ConfigurationResponse();
        response.setConfigurationId(sapResponse.getId());
        response.setProductId(sapResponse.getProductKey());
        response.setKbId(sapResponse.getKbId() != null ? String.valueOf(sapResponse.getKbId()) : null);
        response.setComplete(sapResponse.isComplete());
        response.setConsistent(sapResponse.isConsistent());

        if (sapResponse.getRootItem() != null) {
            response.setRootItem(mapRuntimeItem(sapResponse.getRootItem()));
        }

        return response;
    }

    private static ConfigurationItem mapRuntimeItem(SapRuntimeRootItem sapItem) {
        if (sapItem == null) {
            return null;
        }

        ConfigurationItem item = new ConfigurationItem();
        item.setId(sapItem.getId());
        item.setKey(sapItem.getKey());
        item.setComplete(sapItem.isComplete());
        item.setConsistent(sapItem.isConsistent());

        if (sapItem.getCharacteristics() != null) {
            item.setCharacteristics(
                    sapItem.getCharacteristics().stream()
                            .map(ExternalConfigurationMapper::mapRuntimeCharacteristic)
                            .toList()
            );
        } else {
            item.setCharacteristics(new ArrayList<>());
        }

        if (sapItem.getSubItems() != null) {
            item.setSubItems(
                    sapItem.getSubItems().stream()
                            .map(ExternalConfigurationMapper::mapRuntimeItem)
                            .toList()
            );
        } else {
            item.setSubItems(new ArrayList<>());
        }

        return item;
    }

    private static CharacteristicDto mapRuntimeCharacteristic(SapRuntimeCharacteristic sapCharacteristic) {
        if (sapCharacteristic == null) {
            return null;
        }

        CharacteristicDto characteristic = new CharacteristicDto();
        characteristic.setId(sapCharacteristic.getId());
        characteristic.setReadOnly(sapCharacteristic.isReadOnly());
        characteristic.setRequired(sapCharacteristic.isRequired());
        characteristic.setVisible(sapCharacteristic.isVisible());
        characteristic.setConsistent(sapCharacteristic.isConsistent());
        characteristic.setComplete(sapCharacteristic.isComplete());

        if (sapCharacteristic.getValues() != null) {
            characteristic.setValues(
                    sapCharacteristic.getValues().stream()
                            .map(ExternalConfigurationMapper::mapRuntimeValue)
                            .toList()
            );
        } else {
            characteristic.setValues(new ArrayList<>());
        }

        if (sapCharacteristic.getPossibleValues() != null) {
            characteristic.setPossibleValues(
                    sapCharacteristic.getPossibleValues().stream()
                            .map(ExternalConfigurationMapper::mapRuntimePossibleValue)
                            .toList()
            );
        } else {
            characteristic.setPossibleValues(new ArrayList<>());
        }

        return characteristic;
    }

    private static CharacteristicValueDto mapRuntimeValue(SapRuntimeValue sapValue) {
        if (sapValue == null) {
            return null;
        }

        CharacteristicValueDto value = new CharacteristicValueDto();
        value.setId(sapValue.getValue());
        value.setName(sapValue.getValue());
        value.setSelected(true);
        value.setAuthor(sapValue.getAuthor());
        return value;
    }

    private static CharacteristicValueDto mapRuntimePossibleValue(SapRuntimePossibleValue sapValue) {
        if (sapValue == null) {
            return null;
        }

        CharacteristicValueDto value = new CharacteristicValueDto();
        value.setId(sapValue.getValueLow());
        value.setName(sapValue.getValueLow());
        value.setSelected(false);
        value.setAuthor(null);
        return value;
    }
}