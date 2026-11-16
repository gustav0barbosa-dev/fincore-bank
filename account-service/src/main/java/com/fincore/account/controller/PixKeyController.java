package com.fincore.account.controller;

import com.fincore.account.dto.CreatePixKeyRequest;
import com.fincore.account.dto.PixKeyResponse;
import com.fincore.account.service.PixKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pix-keys")
@RequiredArgsConstructor
public class PixKeyController {

    private final PixKeyService pixKeyService;

    @PostMapping
    public ResponseEntity<PixKeyResponse> create(@Valid @RequestBody CreatePixKeyRequest request,
                                                  Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pixKeyService.create(request, auth.getName()));
    }

    @GetMapping
    public ResponseEntity<List<PixKeyResponse>> list(Authentication auth) {
        return ResponseEntity.ok(pixKeyService.listByOwner(auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        pixKeyService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
