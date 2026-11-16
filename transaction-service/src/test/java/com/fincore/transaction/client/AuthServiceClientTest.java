package com.fincore.transaction.client;

import com.fincore.transaction.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("AuthServiceClient - Testes Unitários")
class AuthServiceClientTest {

    private static final String BASE_URL = "http://localhost:8081";

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Deve repassar o JWT ao validar o PIN")
    void validateTransactionPin_forwardsJwt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        authenticate("Bearer token-123");

        server.expect(once(), requestTo(BASE_URL + "/api/v1/auth/transaction-pin/validate"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-123"))
                .andRespond(withSuccess());

        new AuthServiceClient(builder, BASE_URL).validateTransactionPin("1234");

        server.verify();
    }

    @Test
    @DisplayName("Deve converter rejeição do auth-service em erro de negócio")
    void validateTransactionPin_rejected_throwsBusinessException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        authenticate("Bearer token-123");

        server.expect(requestTo(BASE_URL + "/api/v1/auth/transaction-pin/validate"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        AuthServiceClient client = new AuthServiceClient(builder, BASE_URL);

        assertThatThrownBy(() -> client.validateTransactionPin("0000"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("PIN de transação inválido");

        server.verify();
    }

    @Test
    @DisplayName("Deve rejeitar chamada sem usuário autenticado")
    void validateTransactionPin_withoutJwt_throwsBusinessException() {
        RestClient.Builder builder = RestClient.builder();
        AuthServiceClient client = new AuthServiceClient(builder, BASE_URL);

        assertThatThrownBy(() -> client.validateTransactionPin("1234"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Usuário não autenticado");
    }

    private void authenticate(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
