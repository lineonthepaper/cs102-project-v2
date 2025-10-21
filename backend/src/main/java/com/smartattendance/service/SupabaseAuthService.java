package com.smartattendance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupabaseAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseAuthService.class);

    private final RestTemplate restTemplate;
    private final String supabaseBaseUrl;
    private final String serviceRoleKey;

    public SupabaseAuthService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${supabase.api.url}") String supabaseBaseUrl,
            @Value("${supabase.service-role-key:}") String serviceRoleKey
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.supabaseBaseUrl = supabaseBaseUrl != null ? supabaseBaseUrl.replaceAll("/+$", "") : "";
        this.serviceRoleKey = serviceRoleKey;
    }

    public void removeAuthUser(String authId) {
        if (authId == null || authId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authId is required to delete a Supabase Auth user");
        }

        if (serviceRoleKey == null || serviceRoleKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Supabase service role key is not configured on the server");
        }

        final String url = supabaseBaseUrl + "/auth/v1/admin/users/" + authId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceRoleKey);
        headers.set("Authorization", "Bearer " + serviceRoleKey);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
            logger.info("Supabase Auth delete user response status: {}", response.getStatusCode());
        } catch (HttpStatusCodeException httpError) {
            logger.error("Supabase Auth delete user failed: status={}, body={}", httpError.getStatusCode(), httpError.getResponseBodyAsString());
            throw new ResponseStatusException(
                    httpError.getStatusCode(),
                    "Supabase Auth delete failed: " + httpError.getResponseBodyAsString()
            );
        } catch (RestClientException restError) {
            logger.error("Supabase Auth delete user request failed", restError);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to reach Supabase Auth service",
                    restError
            );
        }
    }
}
