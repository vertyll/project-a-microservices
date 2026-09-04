package com.vertyll.veds.notification.infrastructure.config

import com.vertyll.veds.notification.application.port.inbound.NotificationCompensationUseCase
import com.vertyll.veds.notification.application.saga.model.NotificationCompensationCommand
import com.vertyll.veds.notification.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.notification.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.notification.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.notification.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.notification.infrastructure.saga.AvroNotificationCompensationCommandTranslator
import com.vertyll.veds.notification.infrastructure.saga.NotificationCompensationEventSerializer
import com.vertyll.veds.notification.infrastructure.saga.NotificationSagaCompensationHandler
import com.vertyll.veds.notification.infrastructure.saga.NotificationSagaCompensationStepFactory
import com.vertyll.veds.notification.infrastructure.saga.NotificationSagaCompensator
import com.vertyll.veds.notification.infrastructure.saga.NotificationSagaEntityFactory
import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.kafka.KafkaOutboxProcessor
import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.shared.saga.engine.CompensationCommandDeserializer
import com.vertyll.veds.shared.saga.engine.CompensationCommandHandler
import com.vertyll.veds.shared.saga.engine.CompensationEventSerializer
import com.vertyll.veds.shared.saga.engine.DefaultSagaCompensationContext
import com.vertyll.veds.shared.saga.engine.SagaCompensationContext
import com.vertyll.veds.shared.saga.engine.SagaCompensationEngine
import com.vertyll.veds.shared.saga.engine.SagaCompensationRunner
import com.vertyll.veds.shared.saga.engine.SagaCompensationTopic
import com.vertyll.veds.shared.saga.engine.SagaEngine
import com.vertyll.veds.shared.saga.engine.SagaProcessAdapter
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
        const val SAGA_COMPENSATION_TOPIC: String = SagaCompensationTopic.PREFIX + "notification"
    }

    @Bean
    fun notificationSagaCompensationContext(
        kafkaOutboxProcessor: KafkaOutboxProcessor,
        compensationEventSerializer: CompensationEventSerializer<NotificationCompensationCommand>,
        objectMapper: ObjectMapper,
    ): SagaCompensationContext<NotificationCompensationCommand> =
        DefaultSagaCompensationContext(
            kafkaOutboxProcessor = kafkaOutboxProcessor,
            compensationEventSerializer = compensationEventSerializer,
            compensationTopic = SAGA_COMPENSATION_TOPIC,
            objectMapper = objectMapper,
        )

    @Bean
    fun notificationSagaCompensationRunner(
        sagaRepository: SagaJpaRepository,
        sagaStepRepository: SagaStepJpaRepository,
        compensationContext: SagaCompensationContext<NotificationCompensationCommand>,
    ): SagaCompensationRunner<SagaJpaEntity, SagaStepJpaEntity, NotificationCompensationCommand> =
        SagaCompensationRunner(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            compensator = NotificationSagaCompensator(),
            compensationContext = compensationContext,
        )

    @Bean
    fun notificationSagaEngine(
        sagaRepository: SagaJpaRepository,
        sagaStepRepository: SagaStepJpaRepository,
        objectMapper: ObjectMapper,
        compensationRunner: SagaCompensationRunner<SagaJpaEntity, SagaStepJpaEntity, NotificationCompensationCommand>,
    ): SagaEngine<SagaJpaEntity, SagaStepJpaEntity> =
        SagaEngine(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            objectMapper = objectMapper,
            entityFactory = NotificationSagaEntityFactory(),
            compensationRunner = compensationRunner,
        )

    @Bean
    fun notificationSagaProcessPort(engine: SagaEngine<SagaJpaEntity, SagaStepJpaEntity>): SagaProcessPort = SagaProcessAdapter(engine)

    @Bean
    fun notificationSagaCompensationEngine(
        sagaStepRepository: SagaStepJpaRepository,
        commandDeserializer: CompensationCommandDeserializer<NotificationCompensationCommand>,
        handler: CompensationCommandHandler<NotificationCompensationCommand>,
    ): SagaCompensationEngine<SagaStepJpaEntity, NotificationCompensationCommand> =
        SagaCompensationEngine(
            sagaStepRepository = sagaStepRepository,
            commandDeserializer = commandDeserializer,
            stepFactory = NotificationSagaCompensationStepFactory(),
            handler = handler,
        )

    @Bean
    fun notificationCompensationCommandHandler(
        notificationCompensationService: NotificationCompensationUseCase,
    ): CompensationCommandHandler<NotificationCompensationCommand> = NotificationSagaCompensationHandler(notificationCompensationService)

    @Bean
    fun notificationCompensationEventSerializer(
        avroPayloadSerializer: AvroPayloadSerializer,
    ): CompensationEventSerializer<NotificationCompensationCommand> =
        NotificationCompensationEventSerializer(
            avroPayloadSerializer = avroPayloadSerializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun notificationCompensationCommandDeserializer(
        avroPayloadDeserializer: AvroPayloadDeserializer,
    ): CompensationCommandDeserializer<NotificationCompensationCommand> =
        AvroNotificationCompensationCommandTranslator(
            avroPayloadDeserializer = avroPayloadDeserializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun notificationSagaWatchdog(
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
