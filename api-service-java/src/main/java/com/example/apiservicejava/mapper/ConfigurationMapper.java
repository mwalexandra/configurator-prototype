package com.example.apiservicejava.mapper;

import com.example.apiservicejava.model.api.*;
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
            SapKbResponse kb) {
        if (runtime == null) {
            throw new IllegalArgumentException("runtime configuration must not be null");
        }

        ConfigurationResponse response = new ConfigurationResponse();
        response.setConfigurationId(runtime.getId());
        response.setKbId(runtime.getKbId() != null ? runtime.getKbId().toString() : null);
        response.setProductId(resolveProductId(runtime, kb));
        response.setComplete(runtime.isComplete());
        response.setConsistent(runtime.isConsistent());

        if (runtime.getRootItem() != null) {
            response.setRootItem(mapRootItem(runtime.getRootItem(), kb));
        }

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
        if (kb != null && kb.getHeaderInfo() != null && kb.getHeaderInfo().getKey() != null) {
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
        final Map<String, SapKbCharacteristic> kbCharacteristics;

        if (kb != null && kb.getCharacteristics() != null) {
            kbCharacteristics = kb.getCharacteristics()
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(c -> c.getId() != null)
                    .collect(Collectors.toMap(
                            SapKbCharacteristic::getId,
                            Function.identity(),
                            (a, b) -> a));
        } else {
            kbCharacteristics = Collections.emptyMap();
        }

        if (runtimeRoot.getCharacteristics() == null) {
            return Collections.emptyList();
        }

        return runtimeRoot.getCharacteristics()
                .stream()
                .filter(Objects::nonNull)
                .map(runtimeChar -> mapCharacteristic(runtimeChar, kbCharacteristics.get(runtimeChar.getId())))
                .collect(Collectors.toList());
    }

    private CharacteristicDto mapCharacteristic(
            SapRuntimeCharacteristic runtimeChar,
            SapKbCharacteristic kbChar) {
        CharacteristicDto dto = new CharacteristicDto();
        dto.setId(runtimeChar.getId());
        dto.setName(kbChar != null && kbChar.getName() != null ? kbChar.getName() : runtimeChar.getId());
        dto.setDescription(kbChar != null ? kbChar.getDescription() : null);
        dto.setValueType(resolveValueType(runtimeChar, kbChar));
        dto.setRequired(runtimeChar.isRequired());
        dto.setVisible(runtimeChar.isVisible());
        dto.setReadOnly(runtimeChar.isReadOnly());
        dto.setComplete(runtimeChar.isComplete());
        dto.setConsistent(runtimeChar.isConsistent());

        if (kbChar != null) {
            dto.setLength(kbChar.getLength());
            dto.setNumberDecimals(kbChar.getNumberDecimals());
            dto.setEntryFieldMask(kbChar.getEntryFieldMask());
        }

        dto.setValues(mapSelectedValues(runtimeChar, kbChar));
        dto.setPossibleValues(mapPossibleValues(runtimeChar, kbChar));

        return dto;
    }

    private String resolveValueType(SapRuntimeCharacteristic runtimeChar, SapKbCharacteristic kbChar) {
        if (kbChar != null) {
            if ("float".equalsIgnoreCase(kbChar.getType()) || "integer".equalsIgnoreCase(kbChar.getType())) {
                return "NUMERIC";
            }

            if (Boolean.TRUE.equals(kbChar.getMultiValued())) {
                return "MULTI";
            }

            boolean kbFreeText = kbChar.getPossibleValues() == null || kbChar.getPossibleValues().isEmpty();
            if ("string".equalsIgnoreCase(kbChar.getType()) && kbFreeText) {
                return "FREETEXT";
            }

            return "SINGLE";
        }

        boolean freeText = runtimeChar.getPossibleValues() == null
                || runtimeChar.getPossibleValues().isEmpty()
                || runtimeChar.getPossibleValues().stream().allMatch(v -> "0".equals(v.getIntervalType()));

        return freeText ? "FREETEXT" : "SINGLE";
    }

    private List<CharacteristicValueDto> mapSelectedValues(
            SapRuntimeCharacteristic runtimeChar,
            SapKbCharacteristic kbChar) {
        Map<String, SapKbPossibleValue> kbValues = extractKbValues(kbChar);

        if (runtimeChar.getValues() == null) {
            return Collections.emptyList();
        }

        return runtimeChar.getValues().stream()
                .filter(Objects::nonNull)
                .map(v -> {
                    CharacteristicValueDto dto = new CharacteristicValueDto();
                    dto.setId(v.getValue());

                    SapKbPossibleValue kbValue = kbValues.get(v.getValue());
                    dto.setName(kbValue != null && kbValue.getName() != null ? kbValue.getName() : v.getValue());
                    dto.setDescription(kbValue != null ? kbValue.getDescription() : null);

                    dto.setSelected(true);
                    dto.setAuthor(v.getAuthor());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<CharacteristicValueDto> mapPossibleValues(
            SapRuntimeCharacteristic runtimeChar,
            SapKbCharacteristic kbChar) {
        Map<String, SapKbPossibleValue> kbValues = extractKbValues(kbChar);

        if (runtimeChar.getPossibleValues() == null) {
            return Collections.emptyList();
        }

        return runtimeChar.getPossibleValues().stream()
                .filter(Objects::nonNull)
                .filter(SapRuntimePossibleValue::isSelectable)
                .map(v -> {
                    String id = v.getValueLow();
                    if (id == null) {
                        return null;
                    }

                    CharacteristicValueDto dto = new CharacteristicValueDto();
                    dto.setId(id);

                    SapKbPossibleValue kbValue = kbValues.get(id);
                    dto.setName(kbValue != null && kbValue.getName() != null ? kbValue.getName() : id);
                    dto.setDescription(kbValue != null ? kbValue.getDescription() : null);

                    dto.setSelected(false);
                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, SapKbPossibleValue> extractKbValues(SapKbCharacteristic kbChar) {
        if (kbChar == null || kbChar.getPossibleValues() == null) {
            return Collections.emptyMap();
        }

        return kbChar.getPossibleValues().stream()
                .filter(Objects::nonNull)
                .filter(v -> resolveKbValueId(v) != null)
                .collect(Collectors.toMap(
                        this::resolveKbValueId,
                        Function.identity(),
                        (a, b) -> a));
    }

    private String resolveKbValueId(SapKbPossibleValue value) {
        if (value.getId() != null) {
            return value.getId();
        }
        return value.getValueLow();
    }

    private List<CharacteristicGroup> mapGroups(
            SapRuntimeConfigurationResponse runtime,
            SapKbResponse kb) {
        Map<String, String> groupNames = new HashMap<>();

        if (kb != null && kb.getProducts() != null) {
            kb.getProducts().stream()
                    .filter(Objects::nonNull)
                    .filter(p -> Boolean.TRUE.equals(p.getIsRoot()))
                    .findFirst()
                    .ifPresent(rootProduct -> {
                        if (rootProduct.getCharacteristicGroups() != null) {
                            rootProduct.getCharacteristicGroups().stream()
                                    .filter(Objects::nonNull)
                                    .forEach(g -> groupNames.put(g.getId(), g.getName()));
                        }
                    });
        }

        if (runtime.getRootItem() == null || runtime.getRootItem().getCharacteristicGroups() == null) {
            return Collections.emptyList();
        }

        return runtime.getRootItem().getCharacteristicGroups().stream()
                .filter(Objects::nonNull)
                .map(g -> {
                    CharacteristicGroup dto = new CharacteristicGroup();
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
                .filter(Objects::nonNull)
                .map(conflict -> {
                    ConfigurationMessage msg = new ConfigurationMessage();
                    msg.setSeverity("ERROR");
                    msg.setText(conflict.getMessage() != null ? conflict.getMessage() : "Configuration conflict");
                    msg.setCharacteristicId(conflict.getId());
                    return msg;
                })
                .collect(Collectors.toList());
    }
}