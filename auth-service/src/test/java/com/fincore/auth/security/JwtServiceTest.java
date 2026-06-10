package com.fincore.auth.security;

import com.fincore.auth.model.Role;
import com.fincore.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService - Testes Unitários")
class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    private static final String SECRET =
            "8f4b2e9d1c7a3f6e0b5d8a2c4e7f1b3d6a9c2e5f8b1d4a7e0c3f6b9d2a5e8c1f";
    private static final Long EXPIRATION = 86400000L;
    private static final Long REFRESH_EXPIRATION = 604800000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", REFRESH_EXPIRATION);

        user = User.builder()
                .id(1L)
                .fullName("Gustavo Silva")
                .email("gustavo@fincore.com")
                .password("encoded_password")
                .cpf("12345678901")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Deve gerar token JWT válido")
    void generateToken_shouldReturnValidToken() {
        String token = jwtService.generateToken(user);

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Deve extrair username do token corretamente")
    void extractUsername_shouldReturnCorrectEmail() {
        String token = jwtService.generateToken(user);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("gustavo@fincore.com");
    }

    @Test
    @DisplayName("Token deve ser válido para o usuário correto")
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateToken(user);

        boolean valid = jwtService.isTokenValid(token, user);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Token não deve ser válido para usuário diferente")
    void isTokenValid_wrongUser_returnsFalse() {
        User otherUser = User.builder()
                .email("maria@fincore.com")
                .password("password")
                .role(Role.USER)
                .enabled(true)
                .build();

        String token = jwtService.generateToken(user);

        boolean valid = jwtService.isTokenValid(token, otherUser);

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve gerar refresh token diferente do access token")
    void generateRefreshToken_shouldBeDifferentFromAccessToken() {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        assertThat(accessToken).isNotEqualTo(refreshToken);
    }

    @Test
    @DisplayName("Refresh token deve ter expiração maior que access token")
    void refreshToken_shouldHaveLongerExpiration() {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        java.util.Date accessExpiration = jwtService.extractExpiration(accessToken);
        java.util.Date refreshExpiration = jwtService.extractExpiration(refreshToken);

        assertThat(refreshExpiration).isAfter(accessExpiration);
    }

    @Test
    @DisplayName("Deve retornar o valor de expiração configurado")
    void getExpiration_shouldReturnConfiguredValue() {
        Long exp = jwtService.getExpiration();

        assertThat(exp).isEqualTo(EXPIRATION);
    }

    @Test
    @DisplayName("Token expirado deve ser inválido")
    void isTokenValid_expiredToken_returnsFalse() throws InterruptedException {
        ReflectionTestUtils.setField(jwtService, "expiration", 1L);
        String token = jwtService.generateToken(user);
        Thread.sleep(10);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, user))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}
