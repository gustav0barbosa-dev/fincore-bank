package com.fincore.account.service;

import com.fincore.account.dto.*;
import com.fincore.account.exception.BusinessException;
import com.fincore.account.model.*;
import com.fincore.account.repository.AccountRepository;
import com.fincore.account.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .agency("0001")
                .ownerEmail(request.getOwnerEmail())
                .ownerName(request.getOwnerName())
                .accountType(request.getAccountType() != null ? request.getAccountType() : AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        accountRepository.save(account);
        log.info("Conta criada: {} para {}", account.getAccountNumber(), account.getOwnerEmail());
        return AccountResponse.from(account);
    }

    public AccountResponse getAccount(Long id, String requesterEmail) {
        Account account = findAccountById(id);
        if (!account.getOwnerEmail().equals(requesterEmail)) {
            throw new BusinessException("Acesso negado");
        }
        return AccountResponse.from(account);
    }

    public AccountResponse getAccountByNumber(String number) {
        return AccountResponse.from(accountRepository.findByAccountNumber(number)
                .orElseThrow(() -> new BusinessException("Conta não encontrada: " + number)));
    }

    public List<AccountResponse> getAccountsByEmail(String email) {
        return accountRepository.findByOwnerEmail(email)
                .stream().map(AccountResponse::from).toList();
    }

    @Transactional
    public AccountResponse deposit(Long id, DepositRequest request, String requesterEmail) {
        Account account = findAccountById(id);
        if (!account.getOwnerEmail().equals(requesterEmail)) {
            throw new BusinessException("Acesso negado");
        }
        validateActive(account);

        var balanceBefore = account.getBalance();
        account.setBalance(balanceBefore.add(request.getAmount()));
        accountRepository.save(account);

        saveTransaction(account, TransactionType.DEPOSIT, request.getAmount(),
                balanceBefore, account.getBalance(), request.getDescription(), null);

        log.info("Depósito de {} na conta {}", request.getAmount(), account.getAccountNumber());
        return AccountResponse.from(account);
    }

    public Page<TransactionResponse> getStatement(Long id, Pageable pageable, String requesterEmail) {
        Account account = findAccountById(id);
        if (!account.getOwnerEmail().equals(requesterEmail)) {
            throw new BusinessException("Acesso negado");
        }
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(id, pageable)
                .map(TransactionResponse::from);
    }

    private Account findAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Conta não encontrada: " + id));
    }

    private void validateActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Conta não está ativa");
        }
    }

    private void saveTransaction(Account account, TransactionType type,
                                  java.math.BigDecimal amount,
                                  java.math.BigDecimal before,
                                  java.math.BigDecimal after,
                                  String description, String counterpartKey) {
        transactionRepository.save(Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .account(account)
                .type(type)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .description(description)
                .counterpartKey(counterpartKey)
                .status(TransactionStatus.COMPLETED)
                .build());
    }

    @Transactional
    public void debitForPix(Long accountId, BigDecimal amount, String transactionId, String receiverKey) {
        Account account = findAccountById(accountId);
        validateActive(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Saldo insuficiente para PIX");
        }

        var balanceBefore = account.getBalance();
        account.setBalance(balanceBefore.subtract(amount));
        accountRepository.save(account);

        saveTransaction(account, TransactionType.PIX_OUT, amount,
                balanceBefore, account.getBalance(), "PIX para " + receiverKey, transactionId);

        log.info("Débito PIX de {} da conta {} | transactionId: {}",
                amount, account.getAccountNumber(), transactionId);
    }

    private String generateAccountNumber() {
        String number;
        do {
            number = String.format("%08d", SECURE_RANDOM.nextInt(100_000_000));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
