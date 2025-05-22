package com.vertyll.projecta.role.infrastructure.config

import com.vertyll.projecta.common.kafka.KafkaTopicsConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafka

@Configuration
@EnableKafka
@Import(KafkaTopicsConfig::class)
class KafkaConfig {
    // Export the KafkaTopicsConfig as a Bean to ensure it's available
    @Bean
    fun kafkaTopicsConfig(): KafkaTopicsConfig {
        return KafkaTopicsConfig()
    }
}
