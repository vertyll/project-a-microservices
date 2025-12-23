package com.vertyll.projecta.sharedinfrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

/**
 * Common Jackson configuration for all microservices.
 * This configuration is used to customize the ObjectMapper
 */
@Configuration
class JacksonConfig {
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper =
        JsonMapper
            .builder()
            .addModule(KotlinModule.Builder().build())
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .findAndAddModules()
            .build()
}
