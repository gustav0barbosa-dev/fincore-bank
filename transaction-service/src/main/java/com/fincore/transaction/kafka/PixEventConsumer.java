package com.fincore.transaction.kafka;

import com.fincore.transaction.dto.PixEvent;
import com.fincore.transaction.model.PixStatus;
import com.fincore.transaction.model.PixTransaction;
import com.fincore.transaction.repository.PixTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PixEventConsumer {

    private final PixTransactionRepository pixTransactionRepository;
    private final PixEventProducer pixEventProducer;

    @KafkaListener(topics = "${kafka.topics.pix-initiated}", groupId = "transaction-service")
    @Transactional
    public void processPixInitiated(PixEvent event) {
        log.info("Processando PIX iniciado: {}", event.getTransactionId());

        PixTransaction txn = pixTransactionRepository
                .findByTransactionId(event.getTransactionId())
                .orElse(null);

        if (txn == null) {
            log.error("Transação não encontrada: {}", event.getTransactionId());
            return;
        }

        // Idempotência: reentrega do Kafka não reprocessa transação já finalizada
        if (txn.getStatus() == PixStatus.COMPLETED || txn.getStatus() == PixStatus.FAILED) {
            log.warn("PIX {} já processado ({}), ignorando mensagem duplicada",
                    event.getTransactionId(), txn.getStatus());
            return;
        }

        try {
            validatePix(event);

            txn.setStatus(PixStatus.PROCESSING);
            pixTransactionRepository.save(txn);

            // Solicita débito ao account-service via evento Kafka
            pixEventProducer.publishPixDebitRequested(event);
            log.info("Débito PIX solicitado ao account-service: {}", event.getTransactionId());

        } catch (Exception e) {
            log.error("Erro ao iniciar PIX {}: {}", event.getTransactionId(), e.getMessage());

            txn.setStatus(PixStatus.FAILED);
            txn.setFailureReason(e.getMessage());
            pixTransactionRepository.save(txn);

            event.setStatus(PixStatus.FAILED.name());
            event.setFailureReason(e.getMessage());
            pixEventProducer.publishPixFailed(event);
        }
    }

    @KafkaListener(topics = "${kafka.topics.pix-debit-confirmed}", groupId = "transaction-service")
    @Transactional
    public void processDebitConfirmed(PixEvent event) {
        log.info("Débito confirmado pelo account-service: {}", event.getTransactionId());

        pixTransactionRepository.findByTransactionId(event.getTransactionId())
                .ifPresentOrElse(txn -> {
                    if (txn.getStatus() == PixStatus.COMPLETED) {
                        log.warn("PIX {} já concluído, ignorando confirmação duplicada", event.getTransactionId());
                        return;
                    }
                    txn.setStatus(PixStatus.COMPLETED);
                    txn.setReceiverAccountId(event.getReceiverAccountId());
                    pixTransactionRepository.save(txn);

                    event.setStatus(PixStatus.COMPLETED.name());
                    pixEventProducer.publishPixCompleted(event);
                    log.info("PIX concluído com sucesso: {}", event.getTransactionId());
                }, () -> log.error("Transação não encontrada ao confirmar débito: {}",
                        event.getTransactionId()));
    }

    @KafkaListener(topics = "${kafka.topics.pix-debit-failed}", groupId = "transaction-service")
    @Transactional
    public void processDebitFailed(PixEvent event) {
        log.warn("Débito rejeitado pelo account-service: {} — {}", event.getTransactionId(),
                event.getFailureReason());

        pixTransactionRepository.findByTransactionId(event.getTransactionId())
                .ifPresentOrElse(txn -> {
                    if (txn.getStatus() == PixStatus.FAILED) {
                        log.warn("PIX {} já falhou, ignorando falha duplicada", event.getTransactionId());
                        return;
                    }
                    txn.setStatus(PixStatus.FAILED);
                    txn.setFailureReason(event.getFailureReason());
                    pixTransactionRepository.save(txn);

                    event.setStatus(PixStatus.FAILED.name());
                    pixEventProducer.publishPixFailed(event);
                    log.warn("PIX marcado como falho: {}", event.getTransactionId());
                }, () -> log.error("Transação não encontrada ao processar falha de débito: {}",
                        event.getTransactionId()));
    }

    private void validatePix(PixEvent event) {
        if (event.getAmount() == null || event.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Valor inválido para PIX");
        }
        if (event.getReceiverKey() == null || event.getReceiverKey().isBlank()) {
            throw new IllegalArgumentException("Chave PIX inválida");
        }
    }
}
