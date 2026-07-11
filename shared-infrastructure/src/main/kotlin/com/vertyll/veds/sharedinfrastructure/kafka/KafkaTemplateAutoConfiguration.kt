package com.vertyll.veds.sharedinfrastructure.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

/**
 * Autoconfiguration for Kafka Producer and Consumer.
 * Only creates beans when Kafka is enabled.
 *
 * Configuration is bound from `spring.kafka.*` via [KafkaInfraProperties], replacing
 * `@Value` lookups. Style consistent with `MailProperties` and `SharedConfigProperties`.
 */
@Configuration
@ConditionalOnProperty(
    name = ["spring.kafka.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(KafkaInfraProperties::class)
internal class KafkaTemplateAutoConfiguration {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        /** Interval between retry attempts in milliseconds. */
        private const val RETRY_INTERVAL_MS = 1000L

        /** Maximum number of retry attempts before sending to DLT. */
        private const val MAX_RETRIES = 3L
    }

    /**
     * Byte-array producer factory used by the outbox: keys are strings,
     * values are pre-serialized bytes (Avro/JSON encoded by the caller).
     */
    @Bean
    fun producerFactory(properties: KafkaInfraProperties): ProducerFactory<String, ByteArray> {
        val configProps =
            mapOf<String, Any>(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to properties.bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java,
            ) + securityProps(properties)
        return DefaultKafkaProducerFactory(configProps)
    }

    /**
     * Connection-security properties shared by every Kafka client built here.
     * Empty for PLAINTEXT (local dev); for SSL adds the protocol and - when
     * configured - the CA truststore (server-cert verification only, no
     * client certificates / mTLS).
     */
    private fun securityProps(properties: KafkaInfraProperties): Map<String, Any> {
        if (properties.security.protocol.equals("PLAINTEXT", ignoreCase = true)) {
            return emptyMap()
        }
        val props =
            mutableMapOf<String, Any>(
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to properties.security.protocol,
            )
        if (properties.ssl.trustStoreLocation.isNotBlank()) {
            props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = properties.ssl.trustStoreLocation
            props[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = properties.ssl.trustStoreType
            props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = properties.ssl.trustStorePassword
        }
        logger.info(
            "Kafka clients configured with security.protocol={} (truststore: {})",
            properties.security.protocol,
            properties.ssl.trustStoreLocation.ifBlank { "none" },
        )
        return props
    }

    /** Shared [KafkaTemplate] used by [KafkaOutboxProcessor]. */
    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, ByteArray>) = KafkaTemplate(producerFactory)

    /**
     * Byte-array consumer factory. Decoding (Avro/JSON) is performed by
     * the application-level listener, not by Kafka.
     */
    @Bean
    fun consumerFactory(properties: KafkaInfraProperties): ConsumerFactory<String, ByteArray> {
        val configProps =
            mapOf<String, Any>(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to properties.bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to properties.consumer.groupId,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to properties.consumer.autoOffsetReset,
            ) + securityProps(properties)
        return DefaultKafkaConsumerFactory(configProps)
    }

    /**
     * Error handler that retries failed messages [MAX_RETRIES] times with a
     * [RETRY_INTERVAL_MS] interval, then publishes to a Dead Letter Topic
     * (original-topic.DLT) via [DeadLetterPublishingRecoverer].
     */
    @Bean
    fun kafkaErrorHandler(kafkaTemplate: KafkaTemplate<String, ByteArray>): CommonErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)
        val backOff = FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES)
        val errorHandler = DefaultErrorHandler(recoverer, backOff)
        logger.info(
            "Kafka error handler configured: {} retries with {}ms interval, then DLT",
            MAX_RETRIES,
            RETRY_INTERVAL_MS,
        )
        return errorHandler
    }

    /**
     * Listener container factory wired with the byte-array
     * [consumerFactory] and the shared [kafkaErrorHandler]
     * (retry → DLT). Used by every `@KafkaListener` in the system.
     */
    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, ByteArray>,
        kafkaErrorHandler: CommonErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, ByteArray> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, ByteArray>()
        factory.setConsumerFactory(consumerFactory)
        factory.setCommonErrorHandler(kafkaErrorHandler)
        return factory
    }
}
