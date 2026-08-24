package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.application.port.inbound.AuthCompensationUseCase
import com.vertyll.veds.iam.application.port.inbound.MailFeedbackUseCase
import com.vertyll.veds.iam.application.port.inbound.command.AuthCommandUseCase
import com.vertyll.veds.iam.application.port.inbound.command.RoleCommandUseCase
import com.vertyll.veds.iam.application.port.inbound.command.SecurityCommandUseCase
import com.vertyll.veds.iam.application.port.inbound.command.UserCommandUseCase
import com.vertyll.veds.iam.application.port.inbound.query.AuthQueryUseCase
import com.vertyll.veds.iam.application.port.inbound.query.PermissionQueryUseCase
import com.vertyll.veds.iam.application.port.inbound.query.RoleQueryUseCase
import com.vertyll.veds.iam.application.port.inbound.query.SecurityQueryUseCase
import com.vertyll.veds.iam.application.port.inbound.query.UserQueryUseCase
import com.vertyll.veds.iam.application.port.outbound.AuthEventPublisherPort
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import com.vertyll.veds.iam.application.port.outbound.SagaProcessPort
import com.vertyll.veds.iam.application.service.AuthCompensationService
import com.vertyll.veds.iam.application.service.MailFeedbackService
import com.vertyll.veds.iam.application.service.command.AuthCommandService
import com.vertyll.veds.iam.application.service.command.RoleCommandService
import com.vertyll.veds.iam.application.service.command.SecurityCommandService
import com.vertyll.veds.iam.application.service.command.UserCommandService
import com.vertyll.veds.iam.application.service.query.AuthQueryService
import com.vertyll.veds.iam.application.service.query.PermissionQueryService
import com.vertyll.veds.iam.application.service.query.RoleQueryService
import com.vertyll.veds.iam.application.service.query.SecurityQueryService
import com.vertyll.veds.iam.application.service.query.UserQueryService
import com.vertyll.veds.iam.domain.repository.PermissionRepository
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.domain.repository.VerificationTokenRepository
import com.vertyll.veds.iam.infrastructure.logging.Slf4jUseCaseLogger
import com.vertyll.veds.iam.infrastructure.transaction.TransactionalUseCaseFactory
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
    fun authCompensationUseCase(
        transactions: TransactionalUseCaseFactory,
        userRepository: UserRepository,
        verificationTokenRepository: VerificationTokenRepository,
        identityProvider: IdentityProviderPort,
    ): AuthCompensationUseCase =
        transactions.wrap(
            AuthCompensationUseCase::class.java,
            AuthCompensationService(
                userRepository,
                verificationTokenRepository,
                identityProvider,
                Slf4jUseCaseLogger(AuthCompensationService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun mailFeedbackUseCase(
        transactions: TransactionalUseCaseFactory,
        sagaProcessPort: SagaProcessPort,
    ): MailFeedbackUseCase =
        transactions.wrap(
            MailFeedbackUseCase::class.java,
            MailFeedbackService(
                sagaProcessPort,
                Slf4jUseCaseLogger(MailFeedbackService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun authCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        verificationTokenRepository: VerificationTokenRepository,
        userRepository: UserRepository,
        roleRepository: RoleRepository,
        identityProvider: IdentityProviderPort,
        authEventPublisher: AuthEventPublisherPort,
        sagaProcessPort: SagaProcessPort,
    ): AuthCommandUseCase =
        transactions.wrap(
            AuthCommandUseCase::class.java,
            AuthCommandService(
                verificationTokenRepository,
                userRepository,
                roleRepository,
                identityProvider,
                authEventPublisher,
                sagaProcessPort,
                Slf4jUseCaseLogger(AuthCommandService::class.java),
            ),
            NO_METHODS,
        )

    @Bean
    fun roleCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        roleRepository: RoleRepository,
        userRepository: UserRepository,
        identityProvider: IdentityProviderPort,
    ): RoleCommandUseCase =
        transactions.wrap(
            RoleCommandUseCase::class.java,
            RoleCommandService(
                roleRepository,
                userRepository,
                identityProvider,
            ),
            NO_METHODS,
        )

    @Bean
    fun securityCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        identityProvider: IdentityProviderPort,
    ): SecurityCommandUseCase =
        transactions.wrap(
            SecurityCommandUseCase::class.java,
            SecurityCommandService(
                identityProvider,
            ),
            NO_METHODS,
        )

    @Bean
    fun userCommandUseCase(
        transactions: TransactionalUseCaseFactory,
        userRepository: UserRepository,
        authEventPublisher: AuthEventPublisherPort,
    ): UserCommandUseCase =
        transactions.wrap(
            UserCommandUseCase::class.java,
            UserCommandService(
                userRepository,
                authEventPublisher,
            ),
            NO_METHODS,
        )

    @Bean
    fun authQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        userRepository: UserRepository,
    ): AuthQueryUseCase =
        transactions.wrap(
            AuthQueryUseCase::class.java,
            AuthQueryService(
                userRepository,
            ),
            ALL_METHODS,
        )

    @Bean
    fun permissionQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        permissionRepository: PermissionRepository,
        roleRepository: RoleRepository,
    ): PermissionQueryUseCase =
        transactions.wrap(
            PermissionQueryUseCase::class.java,
            PermissionQueryService(
                permissionRepository,
                roleRepository,
            ),
            ALL_METHODS,
        )

    @Bean
    fun roleQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        roleRepository: RoleRepository,
        userRepository: UserRepository,
    ): RoleQueryUseCase =
        transactions.wrap(
            RoleQueryUseCase::class.java,
            RoleQueryService(
                roleRepository,
                userRepository,
            ),
            ALL_METHODS,
        )

    @Bean
    fun securityQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        identityProvider: IdentityProviderPort,
    ): SecurityQueryUseCase =
        transactions.wrap(
            SecurityQueryUseCase::class.java,
            SecurityQueryService(
                identityProvider,
            ),
            ALL_METHODS,
        )

    @Bean
    fun userQueryUseCase(
        transactions: TransactionalUseCaseFactory,
        userRepository: UserRepository,
    ): UserQueryUseCase =
        transactions.wrap(
            UserQueryUseCase::class.java,
            UserQueryService(
                userRepository,
            ),
            ALL_METHODS,
        )
}
