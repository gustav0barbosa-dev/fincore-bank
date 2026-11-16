package com.fincore.account.service;

import com.fincore.account.dto.*;
import com.fincore.account.exception.BusinessException;
import com.fincore.account.model.*;
import com.fincore.account.repository.AccountRepository;
import com.fincore.account.repository.PixKeyRepository;
import com.fincore.account.repository.TransactionRepository;
import com.fincore.account.validation.PixKeyValidator;
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
    private final PixKeyRepository pixKeyRepository;

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
        saveTransaction(account, UUID.randomUUID().toString(), type, amount, before, after,
                description, counterpartKey);
    }

    private void saveTransaction(Account account, String transactionId, TransactionType type,
                                  java.math.BigDecimal amount,
                                  java.math.BigDecimal before,
                                  java.math.BigDecimal after,
                                  String description, String counterpartKey) {
        transactionRepository.save(Transaction.builder()
                .transactionId(transactionId)
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

    /**
     * Executa a transferência PIX de forma atômica no banco do account-service:
     * localiza o destinatário pela chave PIX, debita o pagador e credita o destinatário.
     *
     * @return id da conta do destinatário
     */
    @Transactional
    public Long transferForPix(Long senderAccountId, BigDecimal amount, String transactionId,
                               String receiverKey, String senderEmail) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Valor inválido para PIX");
        }

        Account sender = findAccountById(senderAccountId);
        if (!sender.getOwnerEmail().equals(senderEmail)) {
            throw new BusinessException("Acesso negado: conta não pertence ao usuário autenticado");
        }

        // Idempotência: reentrega do Kafka não pode debitar/creditar duas vezes
        if (transactionRepository.existsByTransactionId(transactionId)) {
            log.warn("PIX {} já processado no account-service; ignorando reentrega", transactionId);
            return tryResolveReceiverId(receiverKey);
        }

        validateActive(sender);

        Account receiver = resolveReceiverAccount(receiverKey);
        validateActive(receiver);

        if (sender.getId().equals(receiver.getId())) {
            throw new BusinessException("Não é possível transferir PIX para a mesma conta");
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Saldo insuficiente para PIX");
        }

        var senderBalanceBefore = sender.getBalance();
        var receiverBalanceBefore = receiver.getBalance();

        sender.setBalance(senderBalanceBefore.subtract(amount));
        receiver.setBalance(receiverBalanceBefore.add(amount));
        accountRepository.save(sender);
        accountRepository.save(receiver);

        saveTransaction(sender, transactionId, TransactionType.PIX_OUT, amount,
                senderBalanceBefore, sender.getBalance(),
                truncate("PIX para " + receiverKey), receiverKey);

        saveTransaction(receiver, TransactionType.PIX_IN, amount,
                receiverBalanceBefore, receiver.getBalance(),
                truncate("PIX recebido de " + senderEmail), senderEmail);

        log.info("PIX {} transferido: {} -> {} | R$ {}",
                transactionId, sender.getAccountNumber(), receiver.getAccountNumber(), amount);

        return receiver.getId();
    }

    private Account resolveReceiverAccount(String receiverKey) {
        String normalizedKey = PixKeyValidator.normalizeForLookup(receiverKey);
        var pixKey = pixKeyRepository.findByKeyValue(normalizedKey)
                .orElseThrow(() -> new BusinessException(
                        "Chave PIX não encontrada no FinCore: " + receiverKey));
        return pixKey.getAccount();
    }

    private Long tryResolveReceiverId(String receiverKey) {
        try {
            return resolveReceiverAccount(receiverKey).getId();
        } catch (BusinessException e) {
            return null;
        }
    }

    private static String truncate(String value) {
        return value.length() <= 255 ? value : value.substring(0, 255);
    }

    private String generateAccountNumber() {
        String number;
        do {
            number = String.format("%08d", SECURE_RANDOM.nextInt(100_000_000));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
