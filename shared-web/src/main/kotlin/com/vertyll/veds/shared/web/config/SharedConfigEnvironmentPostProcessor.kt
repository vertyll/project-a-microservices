package com.vertyll.veds.shared.web.config

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.ClassPathResource

/**
 * EnvironmentPostProcessor that automatically loads shared-web-config.yml from the classpath.
 * This eliminates the need for each microservice to manually import it in application.yml.
 *
 * Added **last**, so these are defaults: a service's own `application-*.yml`, a profile or an
 * environment variable all override them. Added first they would win instead, and a service
 * could not change a shared value even in its own file — which is how an integration test kept
 * talking to a real Schema Registry while asking for a mock one.
 */
internal class SharedConfigEnvironmentPostProcessor : EnvironmentPostProcessor {
    private val loader = YamlPropertySourceLoader()

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        val resource = ClassPathResource("shared-web-config.yml")
        if (resource.exists()) {
            val propertySources = loader.load("shared-web-config", resource)
            propertySources.forEach {
                environment.propertySources.addLast(it)
            }
        }
    }
}
