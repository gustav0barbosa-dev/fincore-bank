package com.fincore.auth.controller;

import com.fincore.auth.dto.AuthResponse;
import com.fincore.auth.dto.ChangePasswordRequest;
import com.fincore.auth.dto.ChangeTransactionPinRequest;
import com.fincore.auth.dto.LoginRequest;
import com.fincore.auth.dto.ProfileResponse;
import com.fincore.auth.dto.RefreshTokenRequest;
import com.fincore.auth.dto.RegisterRequest;
import com.fincore.auth.dto.UpdateProfileRequest;
import com.fincore.auth.dto.ValidateTransactionPinRequest;
import com.fincore.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    // ==================== PERFIL ====================

    @GetMapping({"/me", "/profile"})
    public ResponseEntity<ProfileResponse> me(Authentication auth) {
        return ResponseEntity.ok(authService.getProfile(auth.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                          Authentication auth) {
        return ResponseEntity.ok(authService.updateProfile(auth.getName(), request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                Authentication auth) {
        authService.changePassword(auth.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/transaction-pin")
    public ResponseEntity<Void> changeTransactionPin(@Valid @RequestBody ChangeTransactionPinRequest request,
                                                     Authentication auth) {
        authService.changeTransactionPin(auth.getName(), request);
        return ResponseEntity.noContent().build();
    }

    // ==================== PIN DE TRANSAÇÃO ====================

    /**
     * Endpoint interno usado pelo transaction-service para autorizar um PIX.
     * O JWT do usuário é repassado pelo transaction-service.
     */
    @PostMapping("/transaction-pin/validate")
    public ResponseEntity<Void> validateTransactionPin(@Valid @RequestBody ValidateTransactionPinRequest request,
                                                       Authentication auth) {
        authService.validateTransactionPin(auth.getName(), request);
        return ResponseEntity.ok().build();
    }
}
