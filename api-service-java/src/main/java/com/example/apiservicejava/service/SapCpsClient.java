package com.example.apiservicejava.service;

import com.example.apiservicejava.model.sapruntime.SapCreateRequest;
import com.example.apiservicejava.model.sapruntime.SapRuntimeConfigurationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SapCpsClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public SapCpsClient(
            RestTemplate restTemplate,
            @Value("${sap.cps.base-url}") String baseUrl,
            @Value("${sap.cps.api-key}") String apiKey
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public SapRuntimeConfigurationResponse getConfiguration(String configurationId) {
        String url = baseUrl + "/api/v2/configurations/" + configurationId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("APIKey", apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<SapRuntimeConfigurationResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                SapRuntimeConfigurationResponse.class
        );

        return response.getBody();
    }

    public SapGetConfigurationResult getConfigurationWithEtag(String configurationId) {
        String url = baseUrl + "/api/v2/configurations/" + configurationId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("APIKey", apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<SapRuntimeConfigurationResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                SapRuntimeConfigurationResponse.class
        );

        SapGetConfigurationResult result = new SapGetConfigurationResult();
        result.setBody(response.getBody());
        result.setEtag(response.getHeaders().getETag());
        return result;
    }

    public SapRuntimeConfigurationResponse createConfiguration(SapCreateRequest request) {
        String url = baseUrl + "/api/v2/configurations";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("APIKey", apiKey);

        HttpEntity<SapCreateRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<SapRuntimeConfigurationResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                SapRuntimeConfigurationResponse.class
        );

        return response.getBody();
    }

    public SapRuntimeConfigurationResponse patchConfiguration(
            String configurationId,
            String itemId,
            String characteristicId,
            String value,
            String etag
    ) {
        String url = baseUrl + "/api/v2/configurations/" + configurationId
                + "/items/" + itemId
                + "/characteristics/" + characteristicId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("APIKey", apiKey);
        headers.setIfMatch(etag);

        Map<String, Object> valueInput = new HashMap<>();
        valueInput.put("value", value);
        valueInput.put("selected", true);

        Map<String, Object> body = new HashMap<>();
        body.put("values", List.of(valueInput));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<SapRuntimeConfigurationResponse> response = restTemplate.exchange(
                url,
                HttpMethod.PATCH,
                entity,
                SapRuntimeConfigurationResponse.class
        );

        return response.getBody();
    }
}