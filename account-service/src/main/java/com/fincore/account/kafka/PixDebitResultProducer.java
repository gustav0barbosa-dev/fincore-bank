package com.fincore.account.kafka;

import com.fincore.account.dto.PixDebitRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PixDebitResultProducer {

    private final KafkaTemplate<String, PixDebitRequestedEvent> kafkaTemplate;

    @Value("${kafka.topics.pix-debit-confirmed}")
    private String pixDebitConfirmedTopic;

    @Value("${kafka.topics.pix-debit-failed}")
    private String pixDebitFailedTopic;

    public void publishDebitConfirmed(PixDebitRequestedEvent event) {
        log.info("Publicando débito confirmado: {}", event.getTransactionId());
        kafkaTemplate.send(pixDebitConfirmedTopic, event.getTransactionId(), event);
    }

    public void publishDebitFailed(PixDebitRequestedEvent event, String reason) {
        event.setFailureReason(reason);
        log.warn("Publicando débito rejeitado: {} — {}", event.getTransactionId(), reason);
        kafkaTemplate.send(pixDebitFailedTopic, event.getTransactionId(), event);
    }
}
