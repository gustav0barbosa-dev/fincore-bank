package com.fincore.transaction.kafka;

import com.fincore.transaction.dto.PixEvent;
import com.fincore.transaction.model.PixStatus;
import com.fincore.transaction.model.PixTransaction;
import com.fincore.transaction.repository.PixTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PixEventConsumer - Testes Unitários")
class PixEventConsumerTest {

    @Mock
    private PixTransactionRepository pixTransactionRepository;

    @Mock
    private PixEventProducer pixEventProducer;

    @InjectMocks
    private PixEventConsumer pixEventConsumer;

    private PixTransaction transaction;
    private PixEvent event;

    @BeforeEach
    void setUp() {
        transaction = PixTransaction.builder()
                .id(1L)
                .transactionId("pix-1")
                .senderAccountId(10L)
                .senderEmail("gustavo@fincore.com")
                .receiverKey("maria@fincore.com")
                .amount(new BigDecimal("100.00"))
                .status(PixStatus.PROCESSING)
                .build();

        event = PixEvent.builder()
                .transactionId("pix-1")
                .senderAccountId(10L)
                .senderEmail("gustavo@fincore.com")
                .receiverKey("maria@fincore.com")
                .receiverAccountId(20L)
                .amount(new BigDecimal("100.00"))
                .status(PixStatus.PROCESSING.name())
                .build();
    }

    @Test
    @DisplayName("Deve concluir PIX e publicar pix-completed quando débito é confirmado")
    void processDebitConfirmed_success() {
        when(pixTransactionRepository.findByTransactionId("pix-1")).thenReturn(Optional.of(transaction));

        pixEventConsumer.processDebitConfirmed(event);

        assertThat(transaction.getStatus()).isEqualTo(PixStatus.COMPLETED);
        assertThat(transaction.getReceiverAccountId()).isEqualTo(20L);
        verify(pixTransactionRepository).save(transaction);
        verify(pixEventProducer).publishPixCompleted(event);
    }

    @Test
    @DisplayName("Não deve reabrir PIX já falho ao receber confirmação atrasada")
    void processDebitConfirmed_ignoresTerminalFailedTransaction() {
        transaction.setStatus(PixStatus.FAILED);
        when(pixTransactionRepository.findByTransactionId("pix-1")).thenReturn(Optional.of(transaction));

        pixEventConsumer.processDebitConfirmed(event);

        assertThat(transaction.getStatus()).isEqualTo(PixStatus.FAILED);
        verify(pixTransactionRepository, never()).save(any());
        verifyNoInteractions(pixEventProducer);
    }

    @Test
    @DisplayName("Deve marcar PIX como FAILED e publicar pix-failed quando débito falha")
    void processDebitFailed_success() {
        event.setFailureReason("Saldo insuficiente");
        when(pixTransactionRepository.findByTransactionId("pix-1")).thenReturn(Optional.of(transaction));

        pixEventConsumer.processDebitFailed(event);

        assertThat(transaction.getStatus()).isEqualTo(PixStatus.FAILED);
        assertThat(transaction.getFailureReason()).isEqualTo("Saldo insuficiente");
        verify(pixTransactionRepository).save(transaction);
        verify(pixEventProducer).publishPixFailed(event);
    }

    @Test
    @DisplayName("Não deve reverter PIX concluído ao receber falha atrasada")
    void processDebitFailed_ignoresTerminalCompletedTransaction() {
        transaction.setStatus(PixStatus.COMPLETED);
        when(pixTransactionRepository.findByTransactionId("pix-1")).thenReturn(Optional.of(transaction));

        pixEventConsumer.processDebitFailed(event);

        assertThat(transaction.getStatus()).isEqualTo(PixStatus.COMPLETED);
        verify(pixTransactionRepository, never()).save(any());
        verifyNoInteractions(pixEventProducer);
    }
}
