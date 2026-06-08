package com.example.apiservicejava.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

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

        Map<String, Object> body = new HashMap<>();
        body.put("product", productId);
        body.put("knowledgeBase", kbId);
        body.put("language", locale);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );

        return response.getBody();
    }
}