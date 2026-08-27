package com.vertyll.veds.task.infrastructure.config

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
import com.vertyll.veds.task.application.port.inbound.TaskCompensationUseCase
import com.vertyll.veds.task.application.saga.model.TaskCompensationCommand
import com.vertyll.veds.task.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.task.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.task.infrastructure.saga.AvroTaskCompensationCommandTranslator
import com.vertyll.veds.task.infrastructure.saga.TaskCompensationEventSerializer
import com.vertyll.veds.task.infrastructure.saga.TaskSagaCompensationHandler
import com.vertyll.veds.task.infrastructure.saga.TaskSagaCompensationStepFactory
import com.vertyll.veds.task.infrastructure.saga.TaskSagaCompensator
import com.vertyll.veds.task.infrastructure.saga.TaskSagaEntityFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableScheduling
internal class SagaConfig {
    companion object {
        const val SAGA_COMPENSATION_TOPIC: String = SagaCompensationTopic.PREFIX + "task"
    }

    @Bean
    fun taskSagaCompensationContext(
        kafkaOutboxProcessor: KafkaOutboxProcessor,
        compensationEventSerializer: CompensationEventSerializer<TaskCompensationCommand>,
        objectMapper: ObjectMapper,
    ): SagaCompensationContext<TaskCompensationCommand> =
        DefaultSagaCompensationContext(
            kafkaOutboxProcessor = kafkaOutboxProcessor,
            compensationEventSerializer = compensationEventSerializer,
            compensationTopic = SAGA_COMPENSATION_TOPIC,
            objectMapper = objectMapper,
        )

    @Bean
    fun taskSagaCompensationRunner(
        sagaRepository: SagaJpaRepository,
        sagaStepRepository: SagaStepJpaRepository,
        compensationContext: SagaCompensationContext<TaskCompensationCommand>,
    ): SagaCompensationRunner<SagaJpaEntity, SagaStepJpaEntity, TaskCompensationCommand> =
        SagaCompensationRunner(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            compensator = TaskSagaCompensator(),
            compensationContext = compensationContext,
        )

    @Bean
    fun taskSagaEngine(
        sagaRepository: SagaJpaRepository,
        sagaStepRepository: SagaStepJpaRepository,
        objectMapper: ObjectMapper,
        compensationRunner: SagaCompensationRunner<SagaJpaEntity, SagaStepJpaEntity, TaskCompensationCommand>,
    ): SagaEngine<SagaJpaEntity, SagaStepJpaEntity> =
        SagaEngine(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            objectMapper = objectMapper,
            entityFactory = TaskSagaEntityFactory(),
            compensationRunner = compensationRunner,
        )

    @Bean
    fun taskSagaCompensationEngine(
        sagaStepRepository: SagaStepJpaRepository,
        commandDeserializer: CompensationCommandDeserializer<TaskCompensationCommand>,
        handler: CompensationCommandHandler<TaskCompensationCommand>,
    ): SagaCompensationEngine<SagaStepJpaEntity, TaskCompensationCommand> =
        SagaCompensationEngine(
            sagaStepRepository = sagaStepRepository,
            commandDeserializer = commandDeserializer,
            stepFactory = TaskSagaCompensationStepFactory(),
            handler = handler,
        )

    @Bean
    fun taskCompensationCommandHandler(
        taskCompensationService: TaskCompensationUseCase,
    ): CompensationCommandHandler<TaskCompensationCommand> = TaskSagaCompensationHandler(taskCompensationService)

    @Bean
    fun taskCompensationEventSerializer(
        avroPayloadSerializer: AvroPayloadSerializer,
    ): CompensationEventSerializer<TaskCompensationCommand> =
        TaskCompensationEventSerializer(
            avroPayloadSerializer = avroPayloadSerializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun taskCompensationCommandDeserializer(
        avroPayloadDeserializer: AvroPayloadDeserializer,
    ): CompensationCommandDeserializer<TaskCompensationCommand> =
        AvroTaskCompensationCommandTranslator(
            avroPayloadDeserializer = avroPayloadDeserializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun taskSagaWatchdog(
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
