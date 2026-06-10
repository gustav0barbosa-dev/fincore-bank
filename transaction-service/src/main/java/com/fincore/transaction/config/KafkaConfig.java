package com.fincore.transaction.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic pixInitiatedTopic() {
        return TopicBuilder.name("pix-initiated").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic pixCompletedTopic() {
        return TopicBuilder.name("pix-completed").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic pixFailedTopic() {
        return TopicBuilder.name("pix-failed").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic pixDebitRequestedTopic() {
        return TopicBuilder.name("pix-debit-requested").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic pixDebitConfirmedTopic() {
        return TopicBuilder.name("pix-debit-confirmed").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic pixDebitFailedTopic() {
        return TopicBuilder.name("pix-debit-failed").partitions(1).replicas(1).build();
    }
}
