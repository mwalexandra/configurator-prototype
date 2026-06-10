package com.example.apiservicejava.service;

import com.example.apiservicejava.model.sapruntime.SapRuntimeConfigurationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
public class SapCpsClient {

    @Value("${sap.cps.base-url}")
    private String baseUrl;

    @Value("${sap.cps.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public SapRuntimeConfigurationResponse createConfiguration(String productId, String kbId, String locale) {
        String url = baseUrl + "/api/v2/configurations";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("APIKey", apiKey);

        Map<String, Object> source = new HashMap<>();
        source.put("application", "cpq");
        source.put("type", "quote_item");
        source.put("id", "10");

        Map<String, Object> contextEntry = new HashMap<>();
        contextEntry.put("name", "VBAP-VRKME");
        contextEntry.put("value", "EA");

        List<Map<String, Object>> context = new ArrayList<>();
        context.add(contextEntry);

        Map<String, Object> body = new HashMap<>();
        body.put("context", context);
        body.put("date", LocalDate.now().toString());
        body.put("kbId", Integer.valueOf(kbId));
        body.put("productKey", productId);
        body.put("source", source);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<SapRuntimeConfigurationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    SapRuntimeConfigurationResponse.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            System.out.println("SAP CPS status: " + e.getStatusCode());
            System.out.println("SAP CPS response: " + e.getResponseBodyAsString());
            throw e;
        }
    }

    public SapRuntimeConfigurationResponse getConfiguration(String configurationId) {
        String url = baseUrl + "/api/v2/configurations/" + configurationId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("APIKey", apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<SapRuntimeConfigurationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    SapRuntimeConfigurationResponse.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            System.out.println("SAP CPS status: " + e.getStatusCode());
            System.out.println("SAP CPS response: " + e.getResponseBodyAsString());
            throw e;
        }
    }

    public SapRuntimeConfigurationResponse patchConfiguration(
            String configurationId,
            String itemId,
            String characteristicId,
            String value
    ) {
        String url = baseUrl + "/api/v2/configurations/" + configurationId
                + "/items/" + itemId
                + "/characteristics/" + characteristicId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("APIKey", apiKey);

        Map<String, Object> body = new HashMap<>();

        List<Map<String, Object>> values = new ArrayList<>();
        if (value != null && !value.isBlank()) {
            Map<String, Object> valueEntry = new HashMap<>();
            valueEntry.put("value", value);
            values.add(valueEntry);
        }
        body.put("values", values);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<SapRuntimeConfigurationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    request,
                    SapRuntimeConfigurationResponse.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            System.out.println("SAP CPS PATCH status: " + e.getStatusCode());
            System.out.println("SAP CPS PATCH response: " + e.getResponseBodyAsString());
            throw e;
        }
    }
}