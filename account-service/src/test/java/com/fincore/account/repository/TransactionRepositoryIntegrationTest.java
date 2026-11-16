package com.fincore.account.repository;

import com.fincore.account.model.Account;
import com.fincore.account.model.AccountStatus;
import com.fincore.account.model.AccountType;
import com.fincore.account.model.Transaction;
import com.fincore.account.model.TransactionStatus;
import com.fincore.account.model.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("TransactionRepository - Integração com PostgreSQL")
class TransactionRepositoryIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("Deve permitir PIX_OUT e PIX_IN com o mesmo transactionId")
    void allowsPixOutAndPixInWithSameTransactionId() {
        Account sender = saveAccount("sender-" + UUID.randomUUID());
        Account receiver = saveAccount("receiver-" + UUID.randomUUID());
        String transactionId = UUID.randomUUID().toString();

        transactionRepository.save(transaction(sender, transactionId, TransactionType.PIX_OUT));
        transactionRepository.save(transaction(receiver, transactionId, TransactionType.PIX_IN));

        assertThat(transactionRepository.existsByTransactionId(transactionId)).isTrue();
        assertThat(transactionRepository.findByAccountIdOrderByCreatedAtDesc(
                sender.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
        assertThat(transactionRepository.findByAccountIdOrderByCreatedAtDesc(
                receiver.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
    }

    private Account saveAccount(String ownerEmail) {
        return accountRepository.save(Account.builder()
                .accountNumber(String.format("%08d", Math.abs(UUID.randomUUID().hashCode()) % 100_000_000))
                .agency("0001")
                .ownerEmail(ownerEmail)
                .ownerName("Teste Integração")
                .balance(new BigDecimal("100.00"))
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build());
    }

    private Transaction transaction(Account account, String transactionId, TransactionType type) {
        return Transaction.builder()
                .transactionId(transactionId)
                .account(account)
                .type(type)
                .amount(new BigDecimal("10.00"))
                .balanceBefore(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("90.00"))
                .description("Teste")
                .status(TransactionStatus.COMPLETED)
                .build();
    }
}
