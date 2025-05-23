package com.vertyll.projecta.gateway.config

import com.vertyll.projecta.common.kafka.KafkaOutboxProcessor
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Configuration that provides mock beans for components required by * dependencies that we can't fully exclude.
 */
@Configuration
class MockBeansConfig {

    /**
     * Mock KafkaOutboxProcessor to prevent autowiring issues
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(KafkaOutboxProcessor::class)
    fun kafkaOutboxProcessor(): KafkaOutboxProcessor? {
        // Return null to prevent actual instantiation
        return null
    }

    /**
     * Mock EntityManagerFactory to prevent autowiring issues
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = ["entityManagerFactory"])
    fun entityManagerFactory(): Any? {
        // Return null to prevent actual instantiation
        return null
    }
}
