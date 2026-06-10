package com.fincore.account.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic pixDebitRequestedTopic() {
        return TopicBuilder.name("pix-debit-requested").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic pixDebitConfirmedTopic() {
        return TopicBuilder.name("pix-debit-confirmed").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic pixDebitFailedTopic() {
        return TopicBuilder.name("pix-debit-failed").partitions(1).replicas(1).build();
    }
}
