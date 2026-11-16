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
import com.fincore.auth.validation.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final Pattern TRANSACTION_PIN_PATTERN = Pattern.compile("^\\d{4}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String cpf = ValidationUtils.normalizeCpf(request.getCpf());
        String phone = ValidationUtils.normalizePhone(request.getPhone());

        validateRegistrationData(email, cpf, request.getPassword(), phone, request.getTransactionPin());

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email já cadastrado");
        }
        if (userRepository.existsByCpf(cpf)) {
            throw new BusinessException("CPF já cadastrado");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .cpf(cpf)
                .phone(phone)
                .transactionPin(passwordEncoder.encode(request.getTransactionPin()))
                .role(Role.USER)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("Novo usuário registrado: {}", user.getEmail());

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getExpiration())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    private void validateRegistrationData(String email, String cpf, String password,
                                          String phone, String transactionPin) {
        if (!ValidationUtils.isValidEmail(email)) {
            throw new BusinessException("Email inválido");
        }
        if (!ValidationUtils.isValidCpf(cpf)) {
            throw new BusinessException("CPF inválido");
        }
        if (!ValidationUtils.isStrongPassword(password)) {
            throw new BusinessException(
                    "Senha fraca: deve ter no mínimo 8 caracteres, com letras maiúsculas, minúsculas, números e caracteres especiais");
        }
        if (phone != null && !phone.isBlank() && !ValidationUtils.isValidPhone(phone)) {
            throw new BusinessException("Telefone inválido: deve conter 10 ou 11 dígitos");
        }
        validateTransactionPinFormat(transactionPin);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        log.info("Login realizado: {}", user.getEmail());

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getExpiration())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String userEmail = jwtService.extractUsername(request.getRefreshToken());

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        if (!jwtService.isTokenValid(request.getRefreshToken(), user)) {
            throw new BusinessException("Refresh token inválido ou expirado");
        }

        String newAccessToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .expiresIn(jwtService.getExpiration())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    // ==================== PERFIL ====================

    public ProfileResponse getProfile(String email) {
        return ProfileResponse.from(findUserByEmail(email));
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findUserByEmail(email);

        String phone = ValidationUtils.normalizePhone(request.getPhone());
        if (phone != null && !phone.isBlank() && !ValidationUtils.isValidPhone(phone)) {
            throw new BusinessException("Telefone inválido: deve conter 10 ou 11 dígitos");
        }

        user.setFullName(request.getFullName().trim());
        user.setPhone(phone == null || phone.isBlank() ? null : phone);
        userRepository.save(user);
        log.info("Perfil atualizado: {}", user.getEmail());
        return ProfileResponse.from(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findUserByEmail(email);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Senha atual incorreta");
        }
        if (!ValidationUtils.isStrongPassword(request.getNewPassword())) {
            throw new BusinessException(
                    "Senha fraca: deve ter no mínimo 8 caracteres, com letras maiúsculas, minúsculas, números e caracteres especiais");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("Nova senha deve ser diferente da senha atual");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Senha de acesso alterada para: {}", user.getEmail());
    }

    @Transactional
    public void changeTransactionPin(String email, ChangeTransactionPinRequest request) {
        User user = findUserByEmail(email);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Senha de acesso incorreta");
        }
        validateTransactionPinFormat(request.getNewPin());

        if (user.getTransactionPin() != null
                && passwordEncoder.matches(request.getNewPin(), user.getTransactionPin())) {
            throw new BusinessException("Novo PIN deve ser diferente do PIN atual");
        }

        user.setTransactionPin(passwordEncoder.encode(request.getNewPin()));
        userRepository.save(user);
        log.info("PIN de transação alterado para: {}", user.getEmail());
    }

    // ==================== PIN DE TRANSAÇÃO ====================

    public void validateTransactionPin(String email, ValidateTransactionPinRequest request) {
        User user = findUserByEmail(email);

        if (user.getTransactionPin() == null || user.getTransactionPin().isBlank()) {
            throw new BusinessException("PIN de transação não configurado");
        }
        validateTransactionPinFormat(request.getPin());

        if (!passwordEncoder.matches(request.getPin(), user.getTransactionPin())) {
            log.warn("PIN de transação inválido para: {}", email);
            throw new BusinessException("PIN de transação inválido");
        }
    }

    private void validateTransactionPinFormat(String transactionPin) {
        if (transactionPin == null || !TRANSACTION_PIN_PATTERN.matcher(transactionPin).matches()) {
            throw new BusinessException("Senha de transação deve conter exatamente 4 dígitos");
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
