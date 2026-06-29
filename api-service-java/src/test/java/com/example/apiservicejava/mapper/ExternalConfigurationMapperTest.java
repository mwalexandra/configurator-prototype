package com.example.apiservicejava.mapper;

import com.example.apiservicejava.model.api.CharacteristicDto;
import com.example.apiservicejava.model.api.CharacteristicValueDto;
import com.example.apiservicejava.model.api.ConfigurationItem;
import com.example.apiservicejava.model.api.ConfigurationResponse;
import com.example.apiservicejava.model.sapkb.SapKbCharacteristic;
import com.example.apiservicejava.model.sapkb.SapKbPossibleValue;
import com.example.apiservicejava.model.sapkb.SapKbResponse;
import com.example.apiservicejava.model.sapruntime.SapRuntimeConfigurationResponse;
import com.example.apiservicejava.model.sapruntime.SapRuntimeCharacteristic;
import com.example.apiservicejava.model.sapruntime.SapRuntimePossibleValue;
import com.example.apiservicejava.model.sapruntime.SapRuntimeRootItem;
import com.example.apiservicejava.model.sapruntime.SapRuntimeValue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExternalConfigurationMapper - критическая логика KB enrichment.
 * Эти тесты покрывают баги, которые были найдены в production:
 * - Неправильная приоритезация ключей (valueLow vs id)
 * - Pre-filled names блокировали KB enrichment
 * - Recursive enrichment для subitems не работал
 */
class ExternalConfigurationMapperTest {

    @Test
    void shouldMapFromSapRuntimeResponseWithBasicFields() {
        // Given
        SapRuntimeConfigurationResponse sapResponse = new SapRuntimeConfigurationResponse();
        sapResponse.setId("cfg-123");
        sapResponse.setProductKey("CPS_BURGER");
        sapResponse.setKbId(80);
        sapResponse.setComplete(false);
        sapResponse.setConsistent(true);
        
        SapRuntimeRootItem rootItem = new SapRuntimeRootItem();
        rootItem.setId("1");
        rootItem.setKey("CPS_BURGER");
        rootItem.setComplete(false);
        rootItem.setConsistent(true);
        rootItem.setCharacteristics(new ArrayList<>());
        rootItem.setSubItems(new ArrayList<>());
        sapResponse.setRootItem(rootItem);

        // When
        ConfigurationResponse response = ExternalConfigurationMapper.fromSapRuntimeResponse(sapResponse);

        // Then
        assertNotNull(response);
        assertEquals("cfg-123", response.getConfigurationId());
        assertEquals("CPS_BURGER", response.getProductId());
        assertEquals("80", response.getKbId());
        assertFalse(response.isComplete());
        assertTrue(response.isConsistent());
        assertNotNull(response.getRootItem());
        assertNotNull(response.getGroups());
        assertNotNull(response.getMessages());
    }

    @Test
    void shouldMapRuntimeCharacteristicWithNullNames() {
        // Given: SAP runtime characteristic with value "M"
        SapRuntimeConfigurationResponse sapResponse = new SapRuntimeConfigurationResponse();
        sapResponse.setId("cfg-123");
        sapResponse.setProductKey("CPS_BURGER");
        sapResponse.setKbId(80);
        
        SapRuntimeRootItem rootItem = new SapRuntimeRootItem();
        rootItem.setId("1");
        rootItem.setKey("ROOT");
        
        SapRuntimeCharacteristic sapChar = new SapRuntimeCharacteristic();
        sapChar.setId("CPS_OPTION_M");
        sapChar.setReadOnly(false);
        sapChar.setRequired(true);
        sapChar.setVisible(true);
        sapChar.setConsistent(true);
        sapChar.setComplete(true);
        
        SapRuntimeValue sapValue = new SapRuntimeValue();
        sapValue.setValue("M");
        sapValue.setAuthor("USER");
        sapChar.setValues(List.of(sapValue));
        
        SapRuntimePossibleValue sapPossibleValue = new SapRuntimePossibleValue();
        sapPossibleValue.setValueLow("M");
        sapChar.setPossibleValues(List.of(sapPossibleValue));
        
        rootItem.setCharacteristics(List.of(sapChar));
        rootItem.setSubItems(new ArrayList<>());
        sapResponse.setRootItem(rootItem);

        // When
        ConfigurationResponse response = ExternalConfigurationMapper.fromSapRuntimeResponse(sapResponse);

        // Then: Names должны быть NULL для последующего KB enrichment
        CharacteristicDto characteristic = response.getRootItem().getCharacteristics().get(0);
        assertEquals("CPS_OPTION_M", characteristic.getId());
        assertNull(characteristic.getName(), "Name should be null before KB enrichment");
        assertNull(characteristic.getDescription(), "Description should be null before KB enrichment");
        
        CharacteristicValueDto value = characteristic.getValues().get(0);
        assertEquals("M", value.getId());
        assertNull(value.getName(), "Value name should be null before KB enrichment");
        
        CharacteristicValueDto possibleValue = characteristic.getPossibleValues().get(0);
        assertEquals("M", possibleValue.getId());
        assertNull(possibleValue.getName(), "PossibleValue name should be null before KB enrichment");
    }

    @Test
    void shouldEnrichCharacteristicFromKnowledgeBase() {
        // Given: Configuration with bare characteristic (no names)
        ConfigurationResponse response = new ConfigurationResponse();
        response.setConfigurationId("cfg-123");
        response.setProductId("CPS_BURGER");
        response.setKbId("80");
        
        ConfigurationItem rootItem = new ConfigurationItem();
        rootItem.setId("1");
        rootItem.setKey("ROOT");
        
        CharacteristicDto characteristic = new CharacteristicDto();
        characteristic.setId("CPS_OPTION_M");
        characteristic.setName(null);  // No name yet
        characteristic.setDescription(null);  // No description yet
        characteristic.setValueType(null);
        
        CharacteristicValueDto value = new CharacteristicValueDto();
        value.setId("M");
        value.setName(null);  // No name yet
        characteristic.setValues(List.of(value));
        
        CharacteristicValueDto possibleValue1 = new CharacteristicValueDto();
        possibleValue1.setId("S");
        possibleValue1.setName(null);
        
        CharacteristicValueDto possibleValue2 = new CharacteristicValueDto();
        possibleValue2.setId("M");
        possibleValue2.setName(null);
        
        characteristic.setPossibleValues(List.of(possibleValue1, possibleValue2));
        rootItem.setCharacteristics(List.of(characteristic));
        rootItem.setSubItems(new ArrayList<>());
        response.setRootItem(rootItem);
        
        // Given: KB with names
        SapKbResponse kbResponse = new SapKbResponse();
        
        SapKbCharacteristic kbChar = new SapKbCharacteristic();
        kbChar.setId("CPS_OPTION_M");
        kbChar.setName("Menu options");
        kbChar.setDescription("Choose your menu size");
        kbChar.setType("STRING");
        kbChar.setLength(10);
        
        SapKbPossibleValue kbValue1 = new SapKbPossibleValue();
        kbValue1.setId("S");
        kbValue1.setValueLow("S");
        kbValue1.setName("Small Menu");
        kbValue1.setDescription("Small size menu");
        
        SapKbPossibleValue kbValue2 = new SapKbPossibleValue();
        kbValue2.setId("M");
        kbValue2.setValueLow("M");
        kbValue2.setName("Medium Menu");
        kbValue2.setDescription("Medium size menu");
        
        kbChar.setPossibleValues(List.of(kbValue1, kbValue2));
        kbResponse.setCharacteristics(List.of(kbChar));

        // When
        ExternalConfigurationMapper.enrichFromSapKb(response, kbResponse);

        // Then: Names should be enriched
        CharacteristicDto enrichedChar = response.getRootItem().getCharacteristics().get(0);
        assertEquals("Menu options", enrichedChar.getName());
        assertEquals("Choose your menu size", enrichedChar.getDescription());
        // Note: type is resolved by ConfigurationMapper logic, not directly copied
        assertEquals(10, enrichedChar.getLength());
        
        // Selected value should be enriched
        CharacteristicValueDto enrichedValue = enrichedChar.getValues().get(0);
        assertEquals("Medium Menu", enrichedValue.getName());
        
        // Possible values should be enriched
        CharacteristicValueDto enrichedPossible1 = enrichedChar.getPossibleValues().get(0);
        assertEquals("Small Menu", enrichedPossible1.getName());
        
        CharacteristicValueDto enrichedPossible2 = enrichedChar.getPossibleValues().get(1);
        assertEquals("Medium Menu", enrichedPossible2.getName());
    }

    @Test
    void shouldUseValueLowPriorityOverIdForKbMatching() {
        // Given: Configuration with characteristic value
        ConfigurationResponse response = new ConfigurationResponse();
        ConfigurationItem rootItem = new ConfigurationItem();
        CharacteristicDto characteristic = new CharacteristicDto();
        characteristic.setId("CPS_OPTION_M");
        
        CharacteristicValueDto value = new CharacteristicValueDto();
        value.setId("VALUE");  // Runtime value ID
        value.setName(null);
        characteristic.setValues(List.of(value));
        characteristic.setPossibleValues(new ArrayList<>());
        
        rootItem.setCharacteristics(List.of(characteristic));
        rootItem.setSubItems(new ArrayList<>());
        response.setRootItem(rootItem);
        
        // Given: KB with valueLow that matches runtime value
        SapKbResponse kbResponse = new SapKbResponse();
        SapKbCharacteristic kbChar = new SapKbCharacteristic();
        kbChar.setId("CPS_OPTION_M");
        kbChar.setName("Menu options");
        
        SapKbPossibleValue kbValue = new SapKbPossibleValue();
        kbValue.setId("SOME_OTHER_ID");  // KB ID is different
        kbValue.setValueLow("VALUE");  // But valueLow matches runtime!
        kbValue.setName("Correct Value Name");
        
        kbChar.setPossibleValues(List.of(kbValue));
        kbResponse.setCharacteristics(List.of(kbChar));

        // When
        ExternalConfigurationMapper.enrichFromSapKb(response, kbResponse);

        // Then: Should match by valueLow, not by id
        CharacteristicValueDto enrichedValue = response.getRootItem().getCharacteristics().get(0).getValues().get(0);
        assertEquals("Correct Value Name", enrichedValue.getName(), 
                     "Should use valueLow priority for matching");
    }

    @Test
    void shouldEnrichSubItemsRecursively() {
        // Given: Configuration with nested subitems
        ConfigurationResponse response = new ConfigurationResponse();
        
        ConfigurationItem rootItem = new ConfigurationItem();
        rootItem.setId("1");
        
        CharacteristicDto rootChar = new CharacteristicDto();
        rootChar.setId("ROOT_CHAR");
        rootChar.setName(null);
        rootItem.setCharacteristics(List.of(rootChar));
        
        // SubItem level 1
        ConfigurationItem subItem1 = new ConfigurationItem();
        subItem1.setId("2");
        CharacteristicDto subChar1 = new CharacteristicDto();
        subChar1.setId("SUB_CHAR_1");
        subChar1.setName(null);
        subItem1.setCharacteristics(List.of(subChar1));
        subItem1.setSubItems(new ArrayList<>());
        
        rootItem.setSubItems(List.of(subItem1));
        response.setRootItem(rootItem);
        
        // Given: KB for both root and subitem characteristics
        SapKbResponse kbResponse = new SapKbResponse();
        
        SapKbCharacteristic kbRootChar = new SapKbCharacteristic();
        kbRootChar.setId("ROOT_CHAR");
        kbRootChar.setName("Root Characteristic");
        
        SapKbCharacteristic kbSubChar = new SapKbCharacteristic();
        kbSubChar.setId("SUB_CHAR_1");
        kbSubChar.setName("Subitem Characteristic");
        
        kbResponse.setCharacteristics(List.of(kbRootChar, kbSubChar));

        // When
        ExternalConfigurationMapper.enrichFromSapKb(response, kbResponse);

        // Then: Both root and subitem should be enriched
        assertEquals("Root Characteristic", 
                     response.getRootItem().getCharacteristics().get(0).getName());
        assertEquals("Subitem Characteristic", 
                     response.getRootItem().getSubItems().get(0).getCharacteristics().get(0).getName());
    }

    @Test
    void shouldNotOverrideExistingCharacteristicNames() {
        // Given: Configuration with existing name
        ConfigurationResponse response = new ConfigurationResponse();
        ConfigurationItem rootItem = new ConfigurationItem();
        
        CharacteristicDto characteristic = new CharacteristicDto();
        characteristic.setId("CPS_OPTION_M");
        characteristic.setName("Already Set Name");  // Already has a name
        characteristic.setDescription("Already Set Desc");
        
        rootItem.setCharacteristics(List.of(characteristic));
        rootItem.setSubItems(new ArrayList<>());
        response.setRootItem(rootItem);
        
        // Given: KB with different name
        SapKbResponse kbResponse = new SapKbResponse();
        SapKbCharacteristic kbChar = new SapKbCharacteristic();
        kbChar.setId("CPS_OPTION_M");
        kbChar.setName("KB Name");
        kbChar.setDescription("KB Description");
        kbResponse.setCharacteristics(List.of(kbChar));

        // When
        ExternalConfigurationMapper.enrichFromSapKb(response, kbResponse);

        // Then: Should NOT override existing values
        CharacteristicDto result = response.getRootItem().getCharacteristics().get(0);
        assertEquals("Already Set Name", result.getName(), 
                     "Should not override existing name");
        assertEquals("Already Set Desc", result.getDescription(), 
                     "Should not override existing description");
    }

    @Test
    void shouldHandleNullSafely() {
        // Test null response
        ExternalConfigurationMapper.enrichFromSapKb(null, new SapKbResponse());
        
        // Test null KB response
        ConfigurationResponse response = new ConfigurationResponse();
        response.setRootItem(new ConfigurationItem());
        ExternalConfigurationMapper.enrichFromSapKb(response, null);
        
        // Test null rootItem
        response.setRootItem(null);
        ExternalConfigurationMapper.enrichFromSapKb(response, new SapKbResponse());
        
        // Should not throw NPE
        assertTrue(true, "Should handle nulls gracefully");
    }

    @Test
    void shouldInitializeEmptyGroupsAndMessages() {
        // Given
        SapRuntimeConfigurationResponse sapResponse = new SapRuntimeConfigurationResponse();
        sapResponse.setId("cfg-123");
        sapResponse.setProductKey("CPS_BURGER");
        
        SapRuntimeRootItem rootItem = new SapRuntimeRootItem();
        rootItem.setId("1");
        rootItem.setKey("ROOT");
        rootItem.setCharacteristics(new ArrayList<>());
        rootItem.setSubItems(new ArrayList<>());
        sapResponse.setRootItem(rootItem);

        // When
        ConfigurationResponse response = ExternalConfigurationMapper.fromSapRuntimeResponse(sapResponse);

        // Then: Should have empty lists, not null
        assertNotNull(response.getGroups(), "Groups should be initialized to empty list");
        assertNotNull(response.getMessages(), "Messages should be initialized to empty list");
        assertEquals(0, response.getGroups().size());
        assertEquals(0, response.getMessages().size());
    }
}
