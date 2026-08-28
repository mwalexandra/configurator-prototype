package com.example.apiservicejava.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.apiservicejava.model.sapkb.SapKbResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.example.apiservicejava.service.sap.CpsOAuthTokenService;

@Service
public class SapKbClient {

    private static final Logger log = LoggerFactory.getLogger(SapKbClient.class);

    @Value("${sap.cps.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final CpsOAuthTokenService tokenService;

    public SapKbClient(
            RestTemplate restTemplate,
            CpsOAuthTokenService tokenService) {
        this.restTemplate = restTemplate;
        this.tokenService = tokenService;
    }

    public SapKbResponse getKnowledgeBase(String kbId) {
        if (kbId == null || kbId.isBlank()) {
            throw new IllegalArgumentException("kbId must not be null or blank");
        }

        String url = baseUrl + "/api/v2/knowledgebases/" + kbId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenService.getAccessToken());

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
            log.error("SAP KB status: {}", e.getStatusCode());
            log.error("SAP KB response: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
}