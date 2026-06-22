package com.example.apiservicejava.service.sap;

import com.example.apiservicejava.model.sapruntime.SapCreateRequest;
import com.example.apiservicejava.model.sapruntime.SapRuntimeCharacteristic;
import com.example.apiservicejava.model.sapruntime.SapRuntimeCharacteristicGroup;
import com.example.apiservicejava.model.sapruntime.SapRuntimeConfigurationResponse;
import com.example.apiservicejava.model.sapruntime.SapRuntimeRootItem;
import com.example.apiservicejava.model.sapruntime.SapRuntimeValue;
import com.example.apiservicejava.service.sap.support.SapGetConfigurationResult;
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
                        @Value("${sap.cps.api-key}") String apiKey) {
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
                                SapRuntimeConfigurationResponse.class);

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
                SapRuntimeConfigurationResponse.class);

        // Debugging output to check the response from SAP after the GET request
        System.out.println("New Line starts here __SapCpsClient.getConfigurationWithEtag (70)_____");
        System.out.println("GET status=" + response.getStatusCode());
        System.out.println("GET etag=" + response.getHeaders().getETag());

        SapRuntimeConfigurationResponse body = response.getBody();

        System.out.println("GET body is null=" + (body == null));

        if (body != null) {
                System.out.println("runtime.id=" + body.getId());
                System.out.println("runtime.kbId=" + body.getKbId());
                System.out.println("runtime.productKey=" + body.getProductKey());
                System.out.println("runtime.complete=" + body.isComplete());
                System.out.println("runtime.consistent=" + body.isConsistent());
                System.out.println("runtime.conflicts=" + (body.getConflicts() != null ? body.getConflicts().size() : null));

                SapRuntimeRootItem root = body.getRootItem();
                System.out.println("runtime.root is null=" + (root == null));
                
                System.out.println("**************SubItems: ");
                if (root != null && root.getSubItems() != null) {
                for (SapRuntimeRootItem sub : root.getSubItems()) {
                        if (sub == null) {
                        System.out.println("subItem=null");
                        continue;
                        }

                        System.out.println(
                                "sub.id=" + sub.getId()
                                        + ", key=" + sub.getKey()
                                        + ", type=" + sub.getType()
                                        + ", complete=" + sub.isComplete()
                                        + ", consistent=" + sub.isConsistent()
                                        + ", salesRelevant=" + sub.isSalesRelevant()
                                        + ", subItems.size=" + (sub.getSubItems() != null ? sub.getSubItems().size() : null)
                                        + ", characteristics.size=" + (sub.getCharacteristics() != null ? sub.getCharacteristics().size() : null)
                        );

                        if (sub.getCharacteristics() != null) {
                        for (SapRuntimeCharacteristic c : sub.getCharacteristics()) {
                                if (c == null) continue;

                                System.out.println(
                                        "  sub.cstic id=" + c.getId()
                                                + ", required=" + c.isRequired()
                                                + ", visible=" + c.isVisible()
                                                + ", readOnly=" + c.isReadOnly()
                                                + ", complete=" + c.isComplete()
                                                + ", consistent=" + c.isConsistent()
                                                + ", values.size=" + (c.getValues() != null ? c.getValues().size() : null)
                                );

                                if (c.getValues() != null) {
                                for (SapRuntimeValue v : c.getValues()) {
                                        if (v == null) continue;
                                        System.out.println(
                                                "    sub.selected value=" + v.getValue()
                                                        + ", author=" + v.getAuthor()
                                        );
                                }
                                }
                        }
                        }
                }
                }
        }

        SapGetConfigurationResult result = new SapGetConfigurationResult();
        result.setBody(body);
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
                headers.set("APIKey", apiKey);
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

                // Debugging output to check the response from SAP after the PATCH request
                SapRuntimeConfigurationResponse bodyResponse = response.getBody();

                System.out.println("New Line starts here __SapCpsClient.patchConfiguration_____");
                System.out.println("=== SAP PATCH status=" + response.getStatusCode());
                System.out.println("=== SAP PATCH body=" + bodyResponse);
                System.out.println("=== SAP PATCH complete=" + (bodyResponse != null ? bodyResponse.isComplete() : null));
                System.out.println("=== SAP PATCH consistent=" + (bodyResponse != null ? bodyResponse.isConsistent() : null));
                System.out.println("=== SAP PATCH root.complete=" +
                        (bodyResponse != null && bodyResponse.getRootItem() != null ? bodyResponse.getRootItem().isComplete() : null));
                System.out.println("=== SAP PATCH root.consistent=" +
                        (bodyResponse != null && bodyResponse.getRootItem() != null ? bodyResponse.getRootItem().isConsistent() : null));

                return bodyResponse;
        }

        public SapRuntimeConfigurationResponse completeConfiguration(String configurationId) {
                if (configurationId == null || configurationId.isBlank()) {
                        throw new IllegalArgumentException("configurationId must not be blank");
                }

                return getConfiguration(configurationId);
        }

}