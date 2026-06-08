package com.example.apiservicejava.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;

@Service
public class SapCpsClient {

    @Value("${sap.cps.base-url}")
    private String baseUrl;

    @Value("${sap.cps.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String createConfiguration(String productId, String kbId, String locale) {
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

        java.util.List<Map<String, Object>> context = new java.util.ArrayList<>();
        context.add(contextEntry);

        Map<String, Object> body = new HashMap<>();
        body.put("context", context);
        body.put("date", LocalDate.now().toString());
        body.put("kbId", Integer.valueOf(kbId));
        body.put("productKey", productId);
        body.put("source", source);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            System.out.println("SAP CPS status: " + e.getStatusCode());
            System.out.println("SAP CPS response: " + e.getResponseBodyAsString());
            throw e;
        }
    }
}