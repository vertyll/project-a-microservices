package com.vertyll.veds.sharedinfrastructure.kafka

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Type-safe configuration for the shared Kafka infrastructure.
 *
 * Bound from `spring.kafka.*` in `application.yml`, mirroring the keys used by Spring Boot's
 * own Kafka configuration so existing `application-*.yml` files remain compatible.
 *
 * Replaces ad-hoc `@Value("${spring.kafka.bootstrap-servers:...}")` lookups and keeps the
 * style consistent with `MailProperties` (mail-service) and `SharedConfigProperties`.
 */
@ConfigurationProperties(prefix = "spring.kafka")
data class KafkaInfraProperties(
    /** Comma-separated list of Kafka broker addresses (host:port). */
    val bootstrapServers: String = "localhost:29092",
    /** Schema Registry endpoint used by Avro serializer/deserializer. */
    val schemaRegistryUrl: String = "http://localhost:8081",
    val security: Security = Security(),
    val ssl: Ssl = Ssl(),
    val consumer: Consumer = Consumer(),
) {
    data class Consumer(
        /** Kafka consumer group id used by this service. */
        val groupId: String = "default-group",
        /** Where to start reading when no committed offset exists. */
        val autoOffsetReset: String = "earliest",
    )

    /**
     * Broker connection security. Key names mirror Spring Boot's own
     * `spring.kafka.security.*` so env overrides bind identically
     * (SPRING_KAFKA_SECURITY_PROTOCOL / KAFKA_SECURITY_PROTOCOL via yml).
     */
    data class Security(
        /** PLAINTEXT (default, local dev) or SSL (cluster listener :9094). */
        val protocol: String = "PLAINTEXT",
    )

    /**
     * TLS trust for the SSL listener. Scoped to the Kafka client on purpose:
     * a global JVM truststore would break the services' public-TLS calls
     * (Keycloak, Resend). Mirrors `spring.kafka.ssl.*` key names.
     */
    data class Ssl(
        /** Plain filesystem path to the truststore, e.g. /tls-kafka/truststore.p12. */
        val trustStoreLocation: String = "",
        val trustStoreType: String = "PKCS12",
        val trustStorePassword: String = "",
    )
}
