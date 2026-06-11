package com.fincore.account.service;

import com.fincore.account.dto.AccountResponse;
import com.fincore.account.dto.CreateAccountRequest;
import com.fincore.account.dto.DepositRequest;
import com.fincore.account.exception.BusinessException;
import com.fincore.account.model.*;
import com.fincore.account.repository.AccountRepository;
import com.fincore.account.repository.TransactionRepository;

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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService - Testes Unitários")
class AccountServiceTest {

    private static final String OWNER_EMAIL = "gustavo@fincore.com";
    private static final String OTHER_EMAIL  = "outro@fincore.com";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account account;
    private CreateAccountRequest createRequest;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(1L)
                .accountNumber("12345678")
                .agency("0001")
                .ownerEmail(OWNER_EMAIL)
                .ownerName("Gustavo Silva")
                .balance(BigDecimal.ZERO)
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = new CreateAccountRequest();
        createRequest.setOwnerEmail(OWNER_EMAIL);
        createRequest.setOwnerName("Gustavo Silva");
        createRequest.setAccountType(AccountType.CHECKING);
    }

    // ==================== CREATE ACCOUNT ====================

    @Test
    @DisplayName("Deve criar conta com sucesso")
    void createAccount_success() {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.createAccount(createRequest);

        assertThat(response).isNotNull();
        assertThat(response.getOwnerEmail()).isEqualTo(OWNER_EMAIL);
        assertThat(response.getOwnerName()).isEqualTo("Gustavo Silva");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("Deve criar conta com tipo CHECKING por padrão quando tipo não informado")
    void createAccount_defaultTypeChecking() {
        createRequest.setAccountType(null);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(1L);
            a.setCreatedAt(LocalDateTime.now());
            return a;
        });

        AccountResponse response = accountService.createAccount(createRequest);

        assertThat(response.getAccountType()).isEqualTo(AccountType.CHECKING);
    }

    // ==================== GET ACCOUNT ====================

    @Test
    @DisplayName("Deve buscar conta por ID com sucesso quando requester é o dono")
    void getAccount_success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccount(1L, OWNER_EMAIL);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção quando requester não é dono da conta (IDOR)")
    void getAccount_accessDenied_throwsException() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.getAccount(1L, OTHER_EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Acesso negado");
    }

    @Test
    @DisplayName("Deve lançar exceção quando conta não encontrada por ID")
    void getAccount_notFound_throwsException() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(99L, OWNER_EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Conta não encontrada");
    }

    @Test
    @DisplayName("Deve buscar conta por número com sucesso")
    void getAccountByNumber_success() {
        when(accountRepository.findByAccountNumber("12345678")).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountByNumber("12345678");

        assertThat(response.getAccountNumber()).isEqualTo("12345678");
    }

    @Test
    @DisplayName("Deve lançar exceção quando conta não encontrada por número")
    void getAccountByNumber_notFound_throwsException() {
        when(accountRepository.findByAccountNumber("00000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountByNumber("00000000"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Deve buscar contas por email")
    void getAccountsByEmail_success() {
        when(accountRepository.findByOwnerEmail(OWNER_EMAIL)).thenReturn(List.of(account));

        List<AccountResponse> responses = accountService.getAccountsByEmail(OWNER_EMAIL);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getOwnerEmail()).isEqualTo(OWNER_EMAIL);
    }

    // ==================== DEPOSIT ====================

    @Test
    @DisplayName("Deve realizar depósito com sucesso quando requester é o dono")
    void deposit_success() {
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(new BigDecimal("500.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.deposit(1L, depositRequest, OWNER_EMAIL);

        assertThat(response).isNotNull();
        verify(accountRepository).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao depositar em conta de outro usuário (IDOR)")
    void deposit_accessDenied_throwsException() {
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(new BigDecimal("500.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.deposit(1L, depositRequest, OTHER_EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Acesso negado");
    }

    @Test
    @DisplayName("Deve atualizar saldo após depósito")
    void deposit_shouldUpdateBalance() {
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(new BigDecimal("300.00"));

        account.setBalance(new BigDecimal("200.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.deposit(1L, depositRequest, OWNER_EMAIL);

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Deve lançar exceção ao depositar em conta inativa")
    void deposit_inactiveAccount_throwsException() {
        account.setStatus(AccountStatus.INACTIVE);
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(new BigDecimal("100.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.deposit(1L, depositRequest, OWNER_EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está ativa");
    }

    @Test
    @DisplayName("Deve lançar exceção ao depositar em conta bloqueada")
    void deposit_blockedAccount_throwsException() {
        account.setStatus(AccountStatus.BLOCKED);
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(new BigDecimal("100.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.deposit(1L, depositRequest, OWNER_EMAIL))
                .isInstanceOf(BusinessException.class);
    }
}
