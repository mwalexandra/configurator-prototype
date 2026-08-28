package com.example.apiservicejava.service.sap;

import com.example.apiservicejava.model.sapruntime.SapCreateRequest;
import com.example.apiservicejava.model.sapruntime.SapRuntimeConfigurationResponse;
import com.example.apiservicejava.service.sap.support.SapGetConfigurationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SapCpsClient {

        private final RestTemplate restTemplate;
        private final String baseUrl;
        
        private final String uaaUrl;
        private final String clientId;
        private final String clientSecret;

        public SapCpsClient(
                        RestTemplate restTemplate,
                        @Value("${sap.cps.base-url}") String baseUrl,
                        @Value("${sap.cps.uaa-url}") String uaaUrl,
                        @Value("${sap.cps.client-id}") String clientId,
                        @Value("${sap.cps.client-secret}") String clientSecret) {
                this.restTemplate = restTemplate;
                this.baseUrl = baseUrl;
                this.uaaUrl = uaaUrl;
                this.clientId = clientId;
                this.clientSecret = clientSecret;
        }

        private String getAccessToken() {
                String tokenUrl = uaaUrl + "/oauth/token";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("grant_type", "client_credentials");
                form.add("client_id", clientId);
                form.add("client_secret", clientSecret);

                HttpEntity<MultiValueMap<String, String>> request =
                        new HttpEntity<>(form, headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                        tokenUrl,
                        HttpMethod.POST,
                        request,
                        Map.class);

                Object accessToken = response.getBody().get("access_token");

                if (accessToken == null) {
                        throw new IllegalStateException(
                                "CPS OAuth response contains no access_token");
                }

                return accessToken.toString();
        }

        public SapRuntimeConfigurationResponse getConfiguration(String configurationId) {
                String url = baseUrl + "/api/v2/configurations/" + configurationId;

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(getAccessToken());

                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<SapRuntimeConfigurationResponse> response = restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                entity,
                                SapRuntimeConfigurationResponse.class);

                return response.getBody();
        }

        public SapGetConfigurationResult getConfigurationWithEtag(String configurationId) {
                String url = baseUrl + "/api/v2/configurations/" + configurationId;

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(getAccessToken());

                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<SapRuntimeConfigurationResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        SapRuntimeConfigurationResponse.class);

                SapRuntimeConfigurationResponse body = response.getBody();

                SapGetConfigurationResult result = new SapGetConfigurationResult();
                result.setBody(body);
                result.setEtag(response.getHeaders().getETag());
                return result;
        }

        public SapRuntimeConfigurationResponse createConfiguration(SapCreateRequest request) {
                String url = baseUrl + "/api/v2/configurations";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(getAccessToken());

                HttpEntity<SapCreateRequest> entity = new HttpEntity<>(request, headers);

                ResponseEntity<SapRuntimeConfigurationResponse> response = restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                entity,
                                SapRuntimeConfigurationResponse.class);

                return response.getBody();
        }

        public SapRuntimeConfigurationResponse patchConfiguration(
                        String configurationId,
                        String itemId,
                        String characteristicId,
                        String value,
                        String etag) {
                String url = baseUrl + "/api/v2/configurations/" + configurationId
                                + "/items/" + itemId
                                + "/characteristics/" + characteristicId;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(getAccessToken());
                headers.setIfMatch(etag);

                Map<String, Object> body = new HashMap<>();

                if (value != null) {
                        // Выбор значения
                        Map<String, Object> valueInput = new HashMap<>();
                        valueInput.put("value", value);
                        valueInput.put("selected", true);
                        body.put("values", List.of(valueInput));
                } else {
                        // Снятие выбора — пустой список
                        body.put("values", List.of());
                }

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

                ResponseEntity<SapRuntimeConfigurationResponse> response = restTemplate.exchange(
                                url,
                                HttpMethod.PATCH,
                                entity,
                                SapRuntimeConfigurationResponse.class);

                SapRuntimeConfigurationResponse bodyResponse = response.getBody();
                return bodyResponse;
        }

        public SapRuntimeConfigurationResponse completeConfiguration(String configurationId) {
                if (configurationId == null || configurationId.isBlank()) {
                        throw new IllegalArgumentException("configurationId must not be blank");
                }

                return getConfiguration(configurationId);
        }

        public SapRuntimeConfigurationResponse createConfigurationFromExternal(Map<String, Object> requestBody) {

                String url = baseUrl + "/api/v2/externalConfigurations";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(getAccessToken());

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<SapRuntimeConfigurationResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        SapRuntimeConfigurationResponse.class
                );

                return response.getBody();
        }

        public void deleteConfiguration(String configurationId) {
                if (configurationId == null || configurationId.isBlank()) {
                        throw new IllegalArgumentException("configurationId must not be blank");
                }

                String url = baseUrl + "/api/v2/configurations/" + configurationId;

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(getAccessToken());

                HttpEntity<Void> entity = new HttpEntity<>(headers);

                restTemplate.exchange(
                        url,
                        HttpMethod.DELETE,
                        entity,
                        Void.class);
        }

}