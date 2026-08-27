package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.application.port.inbound.AuthCompensationUseCase
import com.vertyll.veds.iam.application.saga.model.AuthCompensationCommand
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.iam.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.iam.infrastructure.saga.AvroAuthCompensationCommandTranslator
import com.vertyll.veds.iam.infrastructure.saga.IamCompensationEventSerializer
import com.vertyll.veds.iam.infrastructure.saga.IamSagaCompensationHandler
import com.vertyll.veds.iam.infrastructure.saga.IamSagaCompensationStepFactory
import com.vertyll.veds.iam.infrastructure.saga.IamSagaCompensator
import com.vertyll.veds.iam.infrastructure.saga.IamSagaEntityFactory
import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.kafka.KafkaOutboxProcessor
import com.vertyll.veds.shared.saga.engine.CompensationCommandDeserializer
import com.vertyll.veds.shared.saga.engine.CompensationCommandHandler
import com.vertyll.veds.shared.saga.engine.CompensationEventSerializer
import com.vertyll.veds.shared.saga.engine.DefaultSagaCompensationContext
import com.vertyll.veds.shared.saga.engine.SagaCompensationContext
import com.vertyll.veds.shared.saga.engine.SagaCompensationEngine
import com.vertyll.veds.shared.saga.engine.SagaCompensationRunner
import com.vertyll.veds.shared.saga.engine.SagaCompensationTopic
import com.vertyll.veds.shared.saga.engine.SagaEngine
import com.vertyll.veds.shared.saga.engine.SagaProperties
import com.vertyll.veds.shared.saga.engine.SagaWatchdog
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableScheduling
internal class SagaConfig {
    companion object {
        const val SAGA_COMPENSATION_TOPIC: String = SagaCompensationTopic.PREFIX + "iam"
    }

    @Bean
    fun iamSagaCompensationContext(
        kafkaOutboxProcessor: KafkaOutboxProcessor,
        compensationEventSerializer: CompensationEventSerializer<AuthCompensationCommand>,
        objectMapper: ObjectMapper,
    ): SagaCompensationContext<AuthCompensationCommand> =
        DefaultSagaCompensationContext(
            kafkaOutboxProcessor = kafkaOutboxProcessor,
            compensationEventSerializer = compensationEventSerializer,
            compensationTopic = SAGA_COMPENSATION_TOPIC,
            objectMapper = objectMapper,
        )

    @Bean
    fun iamSagaCompensationRunner(
        sagaRepository: SagaJpaRepository,
        sagaStepRepository: SagaStepJpaRepository,
        compensationContext: SagaCompensationContext<AuthCompensationCommand>,
    ): SagaCompensationRunner<SagaJpaEntity, SagaStepJpaEntity, AuthCompensationCommand> =
        SagaCompensationRunner(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            compensator = IamSagaCompensator(),
            compensationContext = compensationContext,
        )

    @Bean
    fun iamSagaEngine(
        sagaRepository: SagaJpaRepository,
        sagaStepRepository: SagaStepJpaRepository,
        objectMapper: ObjectMapper,
        compensationRunner: SagaCompensationRunner<SagaJpaEntity, SagaStepJpaEntity, AuthCompensationCommand>,
    ): SagaEngine<SagaJpaEntity, SagaStepJpaEntity> =
        SagaEngine(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            objectMapper = objectMapper,
            entityFactory = IamSagaEntityFactory(),
            compensationRunner = compensationRunner,
        )

    @Bean
    fun iamSagaCompensationEngine(
        sagaStepRepository: SagaStepJpaRepository,
        commandDeserializer: CompensationCommandDeserializer<AuthCompensationCommand>,
        handler: CompensationCommandHandler<AuthCompensationCommand>,
    ): SagaCompensationEngine<SagaStepJpaEntity, AuthCompensationCommand> =
        SagaCompensationEngine(
            sagaStepRepository = sagaStepRepository,
            commandDeserializer = commandDeserializer,
            stepFactory = IamSagaCompensationStepFactory(),
            handler = handler,
        )

    @Bean
    fun iamCompensationCommandHandler(
        authCompensationService: AuthCompensationUseCase,
    ): CompensationCommandHandler<AuthCompensationCommand> = IamSagaCompensationHandler(authCompensationService)

    @Bean
    fun iamCompensationEventSerializer(
        avroPayloadSerializer: AvroPayloadSerializer,
    ): CompensationEventSerializer<AuthCompensationCommand> =
        IamCompensationEventSerializer(
            avroPayloadSerializer = avroPayloadSerializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun iamCompensationCommandDeserializer(
        avroPayloadDeserializer: AvroPayloadDeserializer,
    ): CompensationCommandDeserializer<AuthCompensationCommand> =
        AvroAuthCompensationCommandTranslator(
            avroPayloadDeserializer = avroPayloadDeserializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun iamSagaWatchdog(
        sagaRepository: SagaJpaRepository,
        sagaEngine: SagaEngine<SagaJpaEntity, SagaStepJpaEntity>,
        sagaProperties: SagaProperties,
    ): SagaWatchdog<SagaJpaEntity, SagaStepJpaEntity> =
        SagaWatchdog(
            sagaRepository = sagaRepository,
            sagaEngine = sagaEngine,
            properties = sagaProperties,
        )
}
