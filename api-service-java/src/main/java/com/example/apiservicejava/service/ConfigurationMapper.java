package com.example.apiservicejava.service;

import com.example.apiservicejava.model.*;
import com.example.apiservicejava.model.sapkb.*;
import com.example.apiservicejava.model.sapruntime.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConfigurationMapper {

    public ConfigurationResponse toWidgetResponse(
            SapRuntimeConfigurationResponse runtime,
            SapKbResponse kb
    ) {
        ConfigurationResponse response = new ConfigurationResponse();
        response.setConfigurationId(runtime.getId());
        response.setKbId(runtime.getKbId() != null ? runtime.getKbId().toString() : null);
        response.setProductId(resolveProductId(runtime, kb));
        response.setComplete(runtime.isComplete());
        response.setConsistent(runtime.isConsistent());
        response.setRootItem(mapRootItem(runtime.getRootItem(), kb));
        response.setGroups(mapGroups(runtime, kb));
        response.setMessages(mapMessages(runtime));
        return response;
    }

    private String resolveProductId(SapRuntimeConfigurationResponse runtime, SapKbResponse kb) {
        if (runtime.getKbKey() != null && runtime.getKbKey().getName() != null) {
            return runtime.getKbKey().getName();
        }
        if (runtime.getProductKey() != null) {
            return runtime.getProductKey();
        }
        if (kb.getHeaderInfo() != null && kb.getHeaderInfo().getKey() != null) {
            return kb.getHeaderInfo().getKey().getName();
        }
        return null;
    }

    private ConfigurationItem mapRootItem(SapRuntimeRootItem runtimeRoot, SapKbResponse kb) {
        ConfigurationItem item = new ConfigurationItem();
        item.setId(runtimeRoot.getId());
        item.setKey(runtimeRoot.getKey());
        item.setComplete(runtimeRoot.isComplete());
        item.setConsistent(runtimeRoot.isConsistent());
        item.setCharacteristics(mapCharacteristics(runtimeRoot, kb));
        item.setSubItems(Collections.emptyList());
        return item;
    }

    private List<CharacteristicDto> mapCharacteristics(SapRuntimeRootItem runtimeRoot, SapKbResponse kb) {
        Map<String, SapKbCharacteristic> kbCharacteristics = kb.getCharacteristics()
                .stream()
                .collect(Collectors.toMap(SapKbCharacteristic::getId, Function.identity(), (a, b) -> a));

        return runtimeRoot.getCharacteristics()
                .stream()
                .map(runtimeChar -> mapCharacteristic(runtimeChar, kbCharacteristics.get(runtimeChar.getId())))
                .collect(Collectors.toList());
    }

    private CharacteristicDto mapCharacteristic(
            SapRuntimeCharacteristic runtimeChar,
            SapKbCharacteristic kbChar
    ) {
        CharacteristicDto dto = new CharacteristicDto();
        dto.setId(runtimeChar.getId());
        dto.setName(kbChar != null ? kbChar.getName() : runtimeChar.getId());
        dto.setDescription(kbChar != null ? kbChar.getDescription() : null);
        dto.setValueType(resolveValueType(runtimeChar, kbChar));
        dto.setRequired(runtimeChar.isRequired());
        dto.setVisible(runtimeChar.isVisible());
        dto.setReadOnly(runtimeChar.isReadOnly());
        dto.setComplete(runtimeChar.isComplete());
        dto.setConsistent(runtimeChar.isConsistent());

        dto.setValues(mapSelectedValues(runtimeChar, kbChar));
        dto.setPossibleValues(mapPossibleValues(runtimeChar, kbChar));

        return dto;
    }

    private String resolveValueType(SapRuntimeCharacteristic runtimeChar, SapKbCharacteristic kbChar) {
        if (kbChar != null) {
            if ("float".equalsIgnoreCase(kbChar.getType())) {
                return "NUMERIC";
            }
            if ("string".equalsIgnoreCase(kbChar.getType()) && Boolean.TRUE.equals(kbChar.getMultiValued())) {
                return "MULTI";
            }
            if ("string".equalsIgnoreCase(kbChar.getType())
                    && (kbChar.getPossibleValues() == null || kbChar.getPossibleValues().isEmpty())) {
                return "FREE_TEXT";
            }
            return "SINGLE";
        }

        boolean freeText = runtimeChar.getPossibleValues() == null
                || runtimeChar.getPossibleValues().isEmpty()
                || runtimeChar.getPossibleValues().stream().allMatch(v -> "0".equals(v.getIntervalType()));

        return freeText ? "FREE_TEXT" : "SINGLE";
    }

    private List<CharacteristicValueDto> mapSelectedValues(
            SapRuntimeCharacteristic runtimeChar,
            SapKbCharacteristic kbChar
    ) {
        Map<String, String> valueNames = extractKbValueNames(kbChar);

        if (runtimeChar.getValues() == null) {
            return Collections.emptyList();
        }

        return runtimeChar.getValues().stream()
                .map(v -> {
                    CharacteristicValueDto dto = new CharacteristicValueDto();
                    dto.setId(v.getValue());
                    dto.setName(valueNames.getOrDefault(v.getValue(), v.getValue()));
                    dto.setSelected(true);
                    dto.setAuthor(v.getAuthor());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<CharacteristicValueDto> mapPossibleValues(
            SapRuntimeCharacteristic runtimeChar,
            SapKbCharacteristic kbChar
    ) {
        Map<String, String> valueNames = extractKbValueNames(kbChar);

        if (runtimeChar.getPossibleValues() == null) {
            return Collections.emptyList();
        }

        return runtimeChar.getPossibleValues().stream()
                .filter(v -> v.isSelectable())
                .map(v -> {
                    String id = v.getValueLow();
                    if (id == null) {
                        return null;
                    }
                    CharacteristicValueDto dto = new CharacteristicValueDto();
                    dto.setId(id);
                    dto.setName(valueNames.getOrDefault(id, id));
                    dto.setSelected(false);
                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, String> extractKbValueNames(SapKbCharacteristic kbChar) {
        if (kbChar == null || kbChar.getPossibleValues() == null) {
            return Collections.emptyMap();
        }

        return kbChar.getPossibleValues().stream()
                .collect(Collectors.toMap(
                        this::resolveKbValueId,
                        v -> v.getName() != null ? v.getName() : resolveKbValueId(v),
                        (a, b) -> a
                ));
    }

    private String resolveKbValueId(SapKbPossibleValue value) {
        if (value.getId() != null) {
            return value.getId();
        }
        return value.getValueLow();
    }

    private List<CharacteristicGroupDto> mapGroups(
            SapRuntimeConfigurationResponse runtime,
            SapKbResponse kb
    ) {
        Map<String, String> groupNames = new HashMap<>();

        if (kb.getProducts() != null) {
            kb.getProducts().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsRoot()))
                    .findFirst()
                    .ifPresent(rootProduct -> {
                        if (rootProduct.getCharacteristicGroups() != null) {
                            rootProduct.getCharacteristicGroups().forEach(g -> groupNames.put(g.getId(), g.getName()));
                        }
                    });
        }

        if (runtime.getRootItem() == null || runtime.getRootItem().getCharacteristicGroups() == null) {
            return Collections.emptyList();
        }

        return runtime.getRootItem().getCharacteristicGroups().stream()
                .map(g -> {
                    CharacteristicGroupDto dto = new CharacteristicGroupDto();
                    dto.setId(g.getId());
                    dto.setName(groupNames.getOrDefault(g.getId(), g.getId()));
                    dto.setComplete(g.isComplete());
                    dto.setConsistent(g.isConsistent());
                    dto.setVisible(g.isVisible());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<ConfigurationMessage> mapMessages(SapRuntimeConfigurationResponse runtime) {
        if (runtime.getConflicts() == null || runtime.getConflicts().isEmpty()) {
            return Collections.emptyList();
        }

        return runtime.getConflicts().stream()
                .map(conflict -> {
                    ConfigurationMessage msg = new ConfigurationMessage();
                    msg.setSeverity("ERROR");
                    msg.setText(conflict.getMessage() != null ? conflict.getMessage() : "Configuration conflict");
                    return msg;
                })
                .collect(Collectors.toList());
    }
}