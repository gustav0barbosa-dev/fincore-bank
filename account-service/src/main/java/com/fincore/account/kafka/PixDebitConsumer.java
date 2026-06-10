package com.fincore.account.kafka;

import com.fincore.account.dto.PixDebitRequestedEvent;
import com.fincore.account.exception.BusinessException;
import com.fincore.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PixDebitConsumer {

    private final AccountService accountService;
    private final PixDebitResultProducer resultProducer;

    @KafkaListener(topics = "${kafka.topics.pix-debit-requested}", groupId = "account-service")
    public void processDebitRequest(PixDebitRequestedEvent event) {
        log.info("Processando débito PIX: {} | R$ {} | conta: {}",
                event.getTransactionId(), event.getAmount(), event.getSenderAccountId());
        try {
            accountService.debitForPix(
                    event.getSenderAccountId(),
                    event.getAmount(),
                    event.getTransactionId(),
                    event.getReceiverKey());
            resultProducer.publishDebitConfirmed(event);
        } catch (BusinessException e) {
            log.error("Débito PIX rejeitado: {} — {}", event.getTransactionId(), e.getMessage());
            resultProducer.publishDebitFailed(event, e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao debitar PIX: {} — {}", event.getTransactionId(), e.getMessage());
            resultProducer.publishDebitFailed(event, "Erro interno ao processar débito");
        }
    }
}
