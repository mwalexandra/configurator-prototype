package com.example.apiservicejava.mapper;

import com.example.apiservicejava.model.api.ConfigurationResponse;
import com.example.apiservicejava.model.sapruntime.SapRuntimeCharacteristic;
import com.example.apiservicejava.model.sapruntime.SapRuntimeConflict;
import com.example.apiservicejava.model.sapruntime.SapRuntimeConfigurationResponse;
import com.example.apiservicejava.model.sapruntime.SapRuntimeRootItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigurationMapperTest {

    @Test
    void shouldMapConflictNameToRuntimeCharacteristicIdWhenConflictIdIsOpaque() {
        SapRuntimeCharacteristic characteristic = new SapRuntimeCharacteristic();
        characteristic.setId("PHALVPAUSFARMSTRUMPF");

        SapRuntimeRootItem rootItem = new SapRuntimeRootItem();
        rootItem.setCharacteristics(List.of(characteristic));

        SapRuntimeConflict conflict = new SapRuntimeConflict();
        conflict.setId("fb56ed49");
        conflict.setName("PHALVPAUSFARMSTRUMPF");
        conflict.setMessage("Value is inconsistent");

        SapRuntimeConfigurationResponse runtime = new SapRuntimeConfigurationResponse();
        runtime.setRootItem(rootItem);
        runtime.setConflicts(List.of(conflict));

        ConfigurationResponse response = new ConfigurationMapper().toWidgetResponse(runtime, null);

        assertEquals("PHALVPAUSFARMSTRUMPF", response.getMessages().get(0).getCharacteristicId());
    }
}