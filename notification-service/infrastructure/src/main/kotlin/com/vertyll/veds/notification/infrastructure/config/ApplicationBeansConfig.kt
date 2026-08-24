package com.vertyll.veds.notification.infrastructure.config

import com.vertyll.veds.notification.application.port.inbound.NotificationCompensationUseCase
import com.vertyll.veds.notification.application.port.inbound.command.NotificationCommandUseCase
import com.vertyll.veds.notification.application.port.inbound.query.NotificationQueryUseCase
import com.vertyll.veds.notification.application.port.outbound.MailRequestPort
import com.vertyll.veds.notification.application.port.outbound.NotificationPushPort
import com.vertyll.veds.notification.application.service.NotificationCompensationService
import com.vertyll.veds.notification.application.service.command.NotificationCommandService
import com.vertyll.veds.notification.application.service.query.NotificationQueryService
import com.vertyll.veds.notification.domain.repository.NotificationRepository
import com.vertyll.veds.notification.domain.repository.NotificationSettingsRepository
import com.vertyll.veds.notification.domain.repository.RecipientDirectoryRepository
import com.vertyll.veds.notification.infrastructure.logging.Slf4jUseCaseLogger
import com.vertyll.veds.notification.infrastructure.transaction.TransactionalUseCaseFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@Suppress("TooManyFunctions", "LongParameterList")
internal class ApplicationBeansConfig {
    private companion object {
        private val ALL_METHODS: (String) -> Boolean = { true }

        private val NO_METHODS: (String) -> Boolean = { false }
    }

    @Bean
    fun notificationCompensationUseCase(
        transactions: TransactionalUseCaseFactory,
        notificationRepository: NotificationRepository,
    ): NotificationCompensationUseCase =
        transactions.wrap(
            NotificationCompensationUseCase::class.java,
            NotificationCompensationService(
                notificationRepository,
                Slf4jUseCaseLogger(NotificationCompensationService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun notificationCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        notificationRepository: NotificationRepository,
        settingsRepository: NotificationSettingsRepository,
        recipients: RecipientDirectoryRepository,
        push: NotificationPushPort,
        mail: MailRequestPort,
    ): NotificationCommandUseCase =
        transactions.wrap(
            NotificationCommandUseCase::class.java,
            NotificationCommandService(
                notificationRepository,
                settingsRepository,
                recipients,
                push,
                mail,
                Slf4jUseCaseLogger(NotificationCommandService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun notificationQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        notificationRepository: NotificationRepository,
        settingsRepository: NotificationSettingsRepository,
    ): NotificationQueryUseCase =
        transactions.wrap(
            NotificationQueryUseCase::class.java,
            NotificationQueryService(
                notificationRepository,
                settingsRepository,
            ),
            ALL_METHODS,
        )
}
