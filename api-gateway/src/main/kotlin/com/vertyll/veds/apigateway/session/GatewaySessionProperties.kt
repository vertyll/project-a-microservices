package com.vertyll.veds.apigateway.session

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "veds.gateway.session")
data class GatewaySessionProperties(
    val encryptionKey: String,
    val ttl: Duration,
)
