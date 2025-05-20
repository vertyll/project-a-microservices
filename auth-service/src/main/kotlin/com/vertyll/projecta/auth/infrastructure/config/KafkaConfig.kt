package com.vertyll.projecta.auth.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka

@Configuration
@EnableKafka
class KafkaConfig {
    // Configuration moved to application.yml
    // Using Spring Boot's autoconfiguration for Kafka
}
