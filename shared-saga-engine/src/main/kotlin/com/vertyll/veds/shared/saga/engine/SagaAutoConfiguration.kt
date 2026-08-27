package com.vertyll.veds.shared.saga.engine

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Autoconfiguration registering the saga [SagaProperties] and enabling scheduling
 * for [SagaWatchdog].
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(SagaProperties::class)
internal class SagaAutoConfiguration
