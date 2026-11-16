package com.fincore.transaction.client;

import com.fincore.transaction.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Cliente HTTP para o auth-service.
 * O JWT do usuário autenticado é repassado para que o auth-service identifique o titular.
 */
@Component
@Slf4j
public class AuthServiceClient {

    private final RestClient restClient;

    public AuthServiceClient(RestClient.Builder restClientBuilder,
                             @Value("${auth.service.url:http://localhost:8081}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public void validateTransactionPin(String pin) {
        String authorization = currentAuthorizationHeader();
        if (authorization == null) {
            throw new BusinessException("Usuário não autenticado");
        }

        try {
            restClient.post()
                    .uri("/api/v1/auth/transaction-pin/validate")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("pin", pin))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("auth-service rejeitou o PIN de transação: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("PIN de transação inválido");
        } catch (ResourceAccessException e) {
            log.error("auth-service indisponível ao validar PIN: {}", e.getMessage());
            throw new BusinessException("Serviço de autenticação indisponível. Tente novamente.");
        }
    }

    private String currentAuthorizationHeader() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }
}
