package com.example.apiservicejava.service.sap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CpsOAuthTokenService {

    private final RestTemplate restTemplate;
    private final String uaaUrl;
    private final String clientId;
    private final String clientSecret;

    public CpsOAuthTokenService(
            RestTemplate restTemplate,
            @Value("${sap.cps.uaa-url}") String uaaUrl,
            @Value("${sap.cps.client-id}") String clientId,
            @Value("${sap.cps.client-secret}") String clientSecret) {
        this.restTemplate = restTemplate;
        this.uaaUrl = uaaUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getAccessToken() {
        String tokenUrl = uaaUrl + "/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                Map.class);

        Object accessToken = response.getBody().get("access_token");
        if (accessToken == null) {
            throw new IllegalStateException(
                    "CPS OAuth response contains no access_token");
        }

        return accessToken.toString();
    }
}