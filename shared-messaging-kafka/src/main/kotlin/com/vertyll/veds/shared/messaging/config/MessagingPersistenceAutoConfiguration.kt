package com.vertyll.veds.shared.messaging.config

import com.vertyll.veds.shared.messaging.kafka.persistence.inbox.ProcessedEventJpaAdapter
import com.vertyll.veds.shared.messaging.kafka.persistence.inbox.ProcessedEventRepository
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxJpaAdapter
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Binds the outbox and inbox ports to their JPA adapters.
 *
 * Each half is registered only where the service enabled the matching
 * repository, which makes `@EnableJpaRepositories` and `@EntityScan` the single
 * place a service states what it carries. A service that consumes but publishes
 * nothing scans the inbox package alone and gets an inbox alone — no outbox
 * bean, and no entity mapped onto a table its migrations never created.
 */
@Configuration
internal class MessagingPersistenceAutoConfiguration {
    @Bean
    @ConditionalOnBean(OutboxRepository::class)
    @ConditionalOnMissingBean(OutboxJpaAdapter::class)
    fun outboxJpaAdapter(repository: OutboxRepository) = OutboxJpaAdapter(repository)

    @Bean
    @ConditionalOnBean(ProcessedEventRepository::class)
    @ConditionalOnMissingBean(ProcessedEventJpaAdapter::class)
    fun processedEventJpaAdapter(repository: ProcessedEventRepository) = ProcessedEventJpaAdapter(repository)
}
