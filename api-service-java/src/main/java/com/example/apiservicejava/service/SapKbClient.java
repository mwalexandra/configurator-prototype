package com.example.apiservicejava.service;

import com.example.apiservicejava.model.sapkb.SapKbResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class SapKbClient {

    @Value("${sap.cps.base-url}")
    private String baseUrl;

    @Value("${sap.cps.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public SapKbClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public SapKbResponse getKnowledgeBase(String kbId) {
        if (kbId == null || kbId.isBlank()) {
            throw new IllegalArgumentException("kbId must not be null or blank");
        }

        String url = baseUrl + "/api/v2/knowledgebases/" + kbId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("APIKey", apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<SapKbResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    SapKbResponse.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            System.out.println("SAP KB status: " + e.getStatusCode());
            System.out.println("SAP KB response: " + e.getResponseBodyAsString());
            throw e;
        }
    }
}