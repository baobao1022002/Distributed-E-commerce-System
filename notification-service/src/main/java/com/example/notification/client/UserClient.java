package com.example.notification.client;

import com.example.notification.client.dto.UserInforDto;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {
    @Qualifier("loadBalancedWebClientBuilder")
    private final WebClient.Builder loadBalancedWebClientBuilder;

    @Qualifier("plainWebClientBuilder")
    private final WebClient.Builder plainWebClientBuilder;

    @Value("${app.clients.auth-service-base-url:http://auth-service}")
    private String authServiceBaseUrl;

    public Flux<UserInforDto> streamAllUsers(int pageSize) {
        if (pageSize <= 0) {
            log.warn("Invalid pageSize={}, fallback to full user fetch", pageSize);
        }

        List<UserInforDto> users = getAllUser();
        Map<String, UserInforDto> uniqueUsers = new LinkedHashMap<>();
        for (UserInforDto user : users) {
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }
            String dedupeKey = (user.getCustomerId() != null && !user.getCustomerId().isBlank())
                    ? user.getCustomerId()
                    : user.getEmail();
            uniqueUsers.putIfAbsent(dedupeKey, user);
        }

        log.info("Streaming {} unique users for notification", uniqueUsers.size());
        return Flux.fromIterable(uniqueUsers.values());
    }

    public List<UserInforDto> getAllUser() {
        String requestUri = authServiceBaseUrl + "/api/users";
        try {
            List<UserInforDto> response = selectBuilder(requestUri).build()
                    .get()
                    .uri(requestUri)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<UserInforDto>>() {
                    })
                    .block(Duration.ofSeconds(10));

            return response != null ? response : Collections.emptyList();
        } catch (WebClientRequestException ex) {
            log.error("Failed to connect to auth-service at {}", requestUri, ex);
            return Collections.emptyList();
        } catch (WebClientResponseException ex) {
            log.error("Failed to fetch users from auth-service. uri={}, status={}, body={}", requestUri, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            return Collections.emptyList();
        } catch (Exception ex) {
            log.error("Failed to fetch users from auth-service at {}", requestUri, ex);
            return Collections.emptyList();
        }
    }

    private WebClient.Builder selectBuilder(String url) {
        URI uri = URI.create(url);
        String host = uri.getHost();
        if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
            return plainWebClientBuilder;
        }
        return loadBalancedWebClientBuilder;
    }
}
