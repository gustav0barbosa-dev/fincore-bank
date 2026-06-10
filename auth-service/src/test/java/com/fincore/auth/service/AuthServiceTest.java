package com.fincore.auth.service;

import com.fincore.auth.dto.AuthResponse;
import com.fincore.auth.dto.LoginRequest;
import com.fincore.auth.dto.RefreshTokenRequest;
import com.fincore.auth.dto.RegisterRequest;
import com.fincore.auth.exception.BusinessException;
import com.fincore.auth.model.Role;
import com.fincore.auth.model.User;
import com.fincore.auth.repository.UserRepository;
import com.fincore.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Testes Unitários")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Gustavo Silva");
        registerRequest.setEmail("gustavo@fincore.com");
        registerRequest.setPassword("Senha@123");
        registerRequest.setCpf("12345678901");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("gustavo@fincore.com");
        loginRequest.setPassword("Senha@123");

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

    // ==================== REGISTER ====================

    @Test
    @DisplayName("Deve registrar usuário com sucesso")
    void register_success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_token");
        when(jwtService.getExpiration()).thenReturn(86400000L);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
        assertThat(response.getEmail()).isEqualTo("gustavo@fincore.com");
        assertThat(response.getFullName()).isEqualTo("Gustavo Silva");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando email já cadastrado")
    void register_emailAlreadyExists_throwsException() {
        when(userRepository.existsByEmail("gustavo@fincore.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email já cadastrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando CPF já cadastrado")
    void register_cpfAlreadyExists_throwsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CPF já cadastrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve codificar a senha ao registrar")
    void register_shouldEncodePassword() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Senha@123")).thenReturn("encoded_password");
        when(userRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(any())).thenReturn("token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtService.getExpiration()).thenReturn(86400000L);

        authService.register(registerRequest);

        verify(passwordEncoder).encode("Senha@123");
    }

    // ==================== LOGIN ====================

    @Test
    @DisplayName("Deve realizar login com sucesso")
    void login_success() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("gustavo@fincore.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_token");
        when(jwtService.getExpiration()).thenReturn(86400000L);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getEmail()).isEqualTo("gustavo@fincore.com");
    }

    @Test
    @DisplayName("Deve chamar AuthenticationManager no login")
    void login_shouldCallAuthenticationManager() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtService.getExpiration()).thenReturn(86400000L);

        authService.login(loginRequest);

        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não encontrado no login")
    void login_userNotFound_throwsException() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ==================== REFRESH TOKEN ====================

    @Test
    @DisplayName("Deve renovar token com sucesso")
    void refreshToken_success() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid_refresh_token");

        when(jwtService.extractUsername("valid_refresh_token")).thenReturn("gustavo@fincore.com");
        when(userRepository.findByEmail("gustavo@fincore.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("valid_refresh_token", user)).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("new_access_token");
        when(jwtService.getExpiration()).thenReturn(86400000L);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new_access_token");
        assertThat(response.getRefreshToken()).isEqualTo("valid_refresh_token");
    }

    @Test
    @DisplayName("Deve lançar exceção quando refresh token inválido")
    void refreshToken_invalidToken_throwsException() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid_token");

        when(jwtService.extractUsername("invalid_token")).thenReturn("gustavo@fincore.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("invalid_token", user)).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Refresh token inválido ou expirado");
    }
}
