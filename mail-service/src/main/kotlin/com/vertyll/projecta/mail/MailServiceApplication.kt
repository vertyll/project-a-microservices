package com.vertyll.projecta.mail

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka
import org.thymeleaf.TemplateEngine
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver

@SpringBootApplication
@EnableJpaRepositories(
    basePackages = [
        "com.vertyll.projecta.mail.domain.repository",
        "com.vertyll.projecta.common.saga",
        "com.vertyll.projecta.common.kafka"
    ]
)
@EntityScan(
    basePackages = [
        "com.vertyll.projecta.mail.domain.model",
        "com.vertyll.projecta.common.saga",
        "com.vertyll.projecta.common.kafka"
    ]
)
@EnableKafka
@ComponentScan(basePackages = ["com.vertyll.projecta.mail", "com.vertyll.projecta.common"])
class MailServiceApplication {

    companion object {
        // Template configuration
        private const val TEMPLATE_PREFIX = "templates/"
        private const val TEMPLATE_SUFFIX = ".html"
        private const val TEMPLATE_ENCODING = "UTF-8"
    }

    /**
     * Configures the Thymeleaf template engine with our template resolver
     */
    @Bean
    fun templateEngine(): TemplateEngine {
        val templateEngine = SpringTemplateEngine()
        templateEngine.addTemplateResolver(templateResolver())
        return templateEngine
    }

    /**
     * Configures the template resolver to find HTML templates in the classpath
     */
    @Bean
    fun templateResolver(): ITemplateResolver {
        val templateResolver = ClassLoaderTemplateResolver()
        templateResolver.prefix = TEMPLATE_PREFIX
        templateResolver.suffix = TEMPLATE_SUFFIX
        templateResolver.templateMode = TemplateMode.HTML
        templateResolver.characterEncoding = TEMPLATE_ENCODING
        templateResolver.isCacheable = false // Set to true in production
        return templateResolver
    }
}

fun main(args: Array<String>) {
    runApplication<MailServiceApplication>(*args)
}
