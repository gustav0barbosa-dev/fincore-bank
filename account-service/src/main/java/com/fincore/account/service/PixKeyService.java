package com.fincore.account.service;

import com.fincore.account.dto.CreatePixKeyRequest;
import com.fincore.account.dto.PixKeyResponse;
import com.fincore.account.exception.BusinessException;
import com.fincore.account.model.Account;
import com.fincore.account.model.AccountStatus;
import com.fincore.account.model.PixKey;
import com.fincore.account.model.PixKeyType;
import com.fincore.account.repository.AccountRepository;
import com.fincore.account.repository.PixKeyRepository;
import com.fincore.account.validation.PixKeyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PixKeyService {

    private final PixKeyRepository pixKeyRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public PixKeyResponse create(CreatePixKeyRequest request, String requesterEmail) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new BusinessException("Conta não encontrada: " + request.getAccountId()));

        if (!account.getOwnerEmail().equals(requesterEmail)) {
            throw new BusinessException("Acesso negado");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Conta não está ativa");
        }

        PixKeyType keyType = request.getKeyType();
        String normalizedValue = PixKeyValidator.normalize(keyType, request.getKeyValue());
        PixKeyValidator.validate(keyType, normalizedValue);

        if (pixKeyRepository.existsByKeyValue(normalizedValue)) {
            throw new BusinessException("Chave PIX já cadastrada");
        }

        PixKey pixKey = PixKey.builder()
                .keyType(keyType)
                .keyValue(normalizedValue)
                .account(account)
                .ownerEmail(requesterEmail)
                .build();

        pixKeyRepository.save(pixKey);
        log.info("Chave PIX {} criada para a conta {}", keyType, account.getAccountNumber());
        return PixKeyResponse.from(pixKey);
    }

    @Transactional(readOnly = true)
    public List<PixKeyResponse> listByOwner(String requesterEmail) {
        return pixKeyRepository.findByOwnerEmailOrderByCreatedAtDesc(requesterEmail)
                .stream()
                .map(PixKeyResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long id, String requesterEmail) {
        PixKey pixKey = pixKeyRepository.findByIdAndOwnerEmail(id, requesterEmail)
                .orElseThrow(() -> new BusinessException("Chave PIX não encontrada: " + id));
        pixKeyRepository.delete(pixKey);
        log.info("Chave PIX {} removida do usuário {}", id, requesterEmail);
    }
}
