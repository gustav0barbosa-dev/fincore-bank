package com.fincore.account.service;

import com.fincore.account.dto.CreatePixKeyRequest;
import com.fincore.account.dto.PixKeyResponse;
import com.fincore.account.exception.BusinessException;
import com.fincore.account.model.Account;
import com.fincore.account.model.AccountStatus;
import com.fincore.account.model.AccountType;
import com.fincore.account.model.PixKey;
import com.fincore.account.model.PixKeyType;
import com.fincore.account.repository.AccountRepository;
import com.fincore.account.repository.PixKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PixKeyService - Testes Unitários")
class PixKeyServiceTest {

    private static final String OWNER_EMAIL = "gustavo@fincore.com";

    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private PixKeyService pixKeyService;

    private Account account;
    private CreatePixKeyRequest request;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(1L)
                .accountNumber("12345678")
                .agency("0001")
                .ownerEmail(OWNER_EMAIL)
                .ownerName("Gustavo Silva")
                .balance(BigDecimal.TEN)
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        request = new CreatePixKeyRequest();
        request.setAccountId(1L);
        request.setKeyType(PixKeyType.CPF);
        request.setKeyValue("529.982.247-25");
    }

    @Test
    @DisplayName("Deve criar chave PIX normalizada")
    void create_success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(pixKeyRepository.existsByKeyValue("52998224725")).thenReturn(false);
        when(pixKeyRepository.save(any(PixKey.class))).thenAnswer(inv -> {
            PixKey key = inv.getArgument(0);
            key.setId(10L);
            key.setCreatedAt(LocalDateTime.now());
            return key;
        });

        PixKeyResponse response = pixKeyService.create(request, OWNER_EMAIL);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getKeyValue()).isEqualTo("52998224725");
        assertThat(response.getAccountId()).isEqualTo(1L);
        assertThat(response.getOwnerEmail()).isEqualTo(OWNER_EMAIL);
        verify(pixKeyRepository).save(any(PixKey.class));
    }

    @Test
    @DisplayName("Deve negar criação de chave para conta de outro usuário")
    void create_accessDenied_throwsException() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> pixKeyService.create(request, "outro@fincore.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Acesso negado");

        verify(pixKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar chave PIX duplicada")
    void create_duplicate_throwsException() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(pixKeyRepository.existsByKeyValue("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> pixKeyService.create(request, OWNER_EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Chave PIX já cadastrada");
    }

    @Test
    @DisplayName("Deve listar chaves PIX do usuário")
    void listByOwner_success() {
        PixKey key = PixKey.builder()
                .id(10L)
                .keyType(PixKeyType.EMAIL)
                .keyValue("gustavo@fincore.com")
                .account(account)
                .ownerEmail(OWNER_EMAIL)
                .createdAt(LocalDateTime.now())
                .build();

        when(pixKeyRepository.findByOwnerEmailOrderByCreatedAtDesc(OWNER_EMAIL))
                .thenReturn(List.of(key));

        List<PixKeyResponse> response = pixKeyService.listByOwner(OWNER_EMAIL);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getKeyValue()).isEqualTo("gustavo@fincore.com");
        assertThat(response.get(0).getAccountNumber()).isEqualTo("12345678");
    }

    @Test
    @DisplayName("Deve remover chave PIX do próprio usuário")
    void delete_success() {
        PixKey key = PixKey.builder().id(10L).ownerEmail(OWNER_EMAIL).build();
        when(pixKeyRepository.findByIdAndOwnerEmail(10L, OWNER_EMAIL)).thenReturn(Optional.of(key));

        pixKeyService.delete(10L, OWNER_EMAIL);

        verify(pixKeyRepository).delete(key);
    }

    @Test
    @DisplayName("Deve negar remoção de chave PIX de outro usuário")
    void delete_notFound_throwsException() {
        when(pixKeyRepository.findByIdAndOwnerEmail(10L, OWNER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pixKeyService.delete(10L, OWNER_EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chave PIX não encontrada");
    }
}
