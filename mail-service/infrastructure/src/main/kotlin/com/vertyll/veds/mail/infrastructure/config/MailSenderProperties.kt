package com.vertyll.veds.mail.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Externalized configuration for outbound mail.
 *
 * Bound from `veds.mail.*` rather than `spring.mail.*`: Spring Boot's `MailProperties` owns that
 * prefix and has no `from` field, so the address could only ever be read as a raw string. Its own
 * namespace makes it a typed, discoverable setting like every other one in this repository.
 *
 * Example:
 * ```yaml
 * veds:
 *   mail:
 *     sender-address: no-reply@veds.local
 * ```
 */
@ConfigurationProperties(prefix = "veds.mail")
data class MailSenderProperties(
    /** `From` address on every message this service sends. */
    val senderAddress: String = DEFAULT_SENDER_ADDRESS,
) {
    companion object {
        const val DEFAULT_SENDER_ADDRESS: String = "no-reply@veds.local"
    }
}
