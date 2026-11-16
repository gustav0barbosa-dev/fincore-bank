package com.fincore.auth.service;

import com.fincore.auth.dto.AuthResponse;
import com.fincore.auth.dto.ChangePasswordRequest;
import com.fincore.auth.dto.ChangeTransactionPinRequest;
import com.fincore.auth.dto.LoginRequest;
import com.fincore.auth.dto.ProfileResponse;
import com.fincore.auth.dto.RefreshTokenRequest;
import com.fincore.auth.dto.RegisterRequest;
import com.fincore.auth.dto.UpdateProfileRequest;
import com.fincore.auth.dto.ValidateTransactionPinRequest;
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
        registerRequest.setCpf("52998224725");
        registerRequest.setTransactionPin("1234");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("gustavo@fincore.com");
        loginRequest.setPassword("Senha@123");

        user = User.builder()
                .id(1L)
                .fullName("Gustavo Silva")
                .email("gustavo@fincore.com")
                .password("encoded_password")
                .cpf("12345678901")
                .transactionPin("encoded_pin")
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
        when(userRepository.existsByCpf("52998224725")).thenReturn(true);

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

    // ==================== PERFIL ====================

    @Test
    @DisplayName("Deve retornar perfil do usuário autenticado")
    void getProfile_success() {
        when(userRepository.findByEmail("gustavo@fincore.com")).thenReturn(Optional.of(user));

        ProfileResponse response = authService.getProfile("gustavo@fincore.com");

        assertThat(response.getEmail()).isEqualTo("gustavo@fincore.com");
        assertThat(response.getFullName()).isEqualTo("Gustavo Silva");
        assertThat(response.isTransactionPinConfigured()).isTrue();
    }

    @Test
    @DisplayName("Deve atualizar nome e telefone do perfil")
    void updateProfile_success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("  Gustavo Souza  ");
        request.setPhone("(11) 99999-9999");

        when(userRepository.findByEmail("gustavo@fincore.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        ProfileResponse response = authService.updateProfile("gustavo@fincore.com", request);

        assertThat(response.getFullName()).isEqualTo("Gustavo Souza");
        assertThat(response.getPhone()).isEqualTo("11999999999");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Deve rejeitar telefone inválido ao atualizar perfil")
    void updateProfile_invalidPhone_throwsException() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Gustavo Souza");
        request.setPhone("123");

        when(userRepository.findByEmail("gustavo@fincore.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.updateProfile("gustavo@fincore.com", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Telefone inválido");

        verify(userRepository, never()).save(any());
    }

    // ==================== ALTERAÇÃO DE SENHA ====================

    @Test
    @DisplayName("Deve alterar senha quando senha atual confere")
    void changePassword_success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Senha@123");
        request.setNewPassword("NovaSenha@456");

        when(userRepository.findByEmail("gustavo@fincore.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha@123", "encoded_password")).thenReturn(true);
        when(passwordEncoder.matches("NovaSenha@456", "encoded_password")).thenReturn(false);
        when(passwordEncoder.encode("NovaSenha@456")).thenReturn("new_encoded_password");

        authService.changePassword("gustavo@fincore.com", request);

        verify(userRepository).save(user);
        assertThat(user.getPassword()).isEqualTo("new_encoded_password");
    }

    @Test
    @DisplayName("Deve rejeitar alteração de senha com senha atual incorreta")
    void changePassword_wrongCurrentPassword_throwsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Errada@123");
        request.setNewPassword("NovaSenha@456");

        when(userRepository.findByEmail("gustavo@fincore.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Errada@123", "encoded_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword("gustavo@fincore.com", request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Senha atual incorreta");

        verify(userRepository, never()).save(any());
    }

    // ==================== PIN DE TRANSAÇÃO ====================

    @Test
    @DisplayName("Deve alterar PIN de transação com a senha de acesso correta")
    void changeTransactionPin_success() {
        ChangeTransactionPinRequest request = new ChangeTransactionPinRequest();
        request.setCurrentPassword("Senha@123");
        request.setNewPin("9876");

        when(userRepository.findByEmail("gustavo@fincore.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha@123", "encoded_password")).thenReturn(true);
        when(passwordEncoder.matches("9876", "encoded_pin")).thenReturn(false);
        when(passwordEncoder.encode("9876")).thenReturn("new_encoded_pin");

        authService.changeTransactionPin("gustavo@fincore.com", request);

        verify(userRepository).save(user);
        assertThat(user.getTransactionPin()).isEqualTo("new_encoded_pin");
    }

    @Test
    @DisplayName("Deve impedir novo PIN igual ao atual")
    void changeTransactionPin_samePin_throwsException() {
        ChangeTransactionPinRequest request = new ChangeTransactionPinRequest();
        request.setCurrentPassword("Senha@123");
        request.setNewPin("1234");

        when(userRepository.findByEmail("gustavo@fincore.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha@123", "encoded_password")).thenReturn(true);
        when(passwordEncoder.matches("1234", "encoded_pin")).thenReturn(true);

        assertThatThrownBy(() -> authService.changeTransactionPin("gustavo@fincore.com", request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Novo PIN deve ser diferente do PIN atual");
    }

    @Test
    @DisplayName("Deve validar PIN de transação correto")
    void validateTransactionPin_success() {
        ValidateTransactionPinRequest request = new ValidateTransactionPinRequest();
        request.setPin("1234");

        when(userRepository.findByEmail("gustavo@fincore.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("1234", "encoded_pin")).thenReturn(true);

        authService.validateTransactionPin("gustavo@fincore.com", request);
    }

    @Test
    @DisplayName("Deve rejeitar PIN de transação incorreto")
    void validateTransactionPin_invalid_throwsException() {
        ValidateTransactionPinRequest request = new ValidateTransactionPinRequest();
        request.setPin("0000");

        when(userRepository.findByEmail("gustavo@fincore.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("0000", "encoded_pin")).thenReturn(false);

        assertThatThrownBy(() -> authService.validateTransactionPin("gustavo@fincore.com", request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("PIN de transação inválido");
    }
}
