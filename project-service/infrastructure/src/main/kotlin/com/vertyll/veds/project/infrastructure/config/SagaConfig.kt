package com.vertyll.veds.project.infrastructure.config

import com.vertyll.veds.project.application.port.inbound.ProjectCompensationUseCase
import com.vertyll.veds.project.application.saga.model.ProjectCompensationCommand
import com.vertyll.veds.project.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.project.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.project.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.project.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.project.infrastructure.saga.AvroProjectCompensationCommandTranslator
import com.vertyll.veds.project.infrastructure.saga.ProjectCompensationEventSerializer
import com.vertyll.veds.project.infrastructure.saga.ProjectSagaCompensationHandler
import com.vertyll.veds.project.infrastructure.saga.ProjectSagaCompensationStepFactory
import com.vertyll.veds.project.infrastructure.saga.ProjectSagaCompensator
import com.vertyll.veds.project.infrastructure.saga.ProjectSagaEntityFactory
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.saga.SagaCompensationTopic
import com.vertyll.veds.sharedinfrastructure.saga.SagaProperties
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationCommandDeserializer
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationCommandHandler
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationEventSerializer
import com.vertyll.veds.sharedinfrastructure.saga.service.DefaultSagaCompensationContext
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationContext
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationEngine
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationRunner
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaEngine
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaWatchdog
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableScheduling
internal class SagaConfig {
    companion object {
        const val SAGA_COMPENSATION_TOPIC: String = SagaCompensationTopic.PREFIX + "project"
    }

    @Bean
    fun projectSagaCompensationContext(
        kafkaOutboxProcessor: KafkaOutboxProcessor,
        compensationEventSerializer: CompensationEventSerializer<ProjectCompensationCommand>,
        objectMapper: ObjectMapper,
    ): SagaCompensationContext<ProjectCompensationCommand> =
        DefaultSagaCompensationContext(
            kafkaOutboxProcessor = kafkaOutboxProcessor,
            compensationEventSerializer = compensationEventSerializer,
            compensationTopic = SAGA_COMPENSATION_TOPIC,
            objectMapper = objectMapper,
        )

    @Bean
    fun projectSagaCompensationRunner(
        sagaRepository: SagaJpaRepository,
        sagaStepRepository: SagaStepJpaRepository,
        compensationContext: SagaCompensationContext<ProjectCompensationCommand>,
    ): SagaCompensationRunner<SagaJpaEntity, SagaStepJpaEntity, ProjectCompensationCommand> =
        SagaCompensationRunner(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            compensator = ProjectSagaCompensator(),
            compensationContext = compensationContext,
        )

    @Bean
    fun projectSagaEngine(
        sagaRepository: SagaJpaRepository,
        sagaStepRepository: SagaStepJpaRepository,
        objectMapper: ObjectMapper,
        compensationRunner: SagaCompensationRunner<SagaJpaEntity, SagaStepJpaEntity, ProjectCompensationCommand>,
    ): SagaEngine<SagaJpaEntity, SagaStepJpaEntity> =
        SagaEngine(
            sagaRepository = sagaRepository,
            sagaStepRepository = sagaStepRepository,
            objectMapper = objectMapper,
            entityFactory = ProjectSagaEntityFactory(),
            compensationRunner = compensationRunner,
        )

    @Bean
    fun projectSagaCompensationEngine(
        sagaStepRepository: SagaStepJpaRepository,
        commandDeserializer: CompensationCommandDeserializer<ProjectCompensationCommand>,
        handler: CompensationCommandHandler<ProjectCompensationCommand>,
    ): SagaCompensationEngine<SagaStepJpaEntity, ProjectCompensationCommand> =
        SagaCompensationEngine(
            sagaStepRepository = sagaStepRepository,
            commandDeserializer = commandDeserializer,
            stepFactory = ProjectSagaCompensationStepFactory(),
            handler = handler,
        )

    @Bean
    fun projectCompensationCommandHandler(
        projectCompensationService: ProjectCompensationUseCase,
    ): CompensationCommandHandler<ProjectCompensationCommand> = ProjectSagaCompensationHandler(projectCompensationService)

    @Bean
    fun projectCompensationEventSerializer(
        avroPayloadSerializer: AvroPayloadSerializer,
    ): CompensationEventSerializer<ProjectCompensationCommand> =
        ProjectCompensationEventSerializer(
            avroPayloadSerializer = avroPayloadSerializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun projectCompensationCommandDeserializer(
        avroPayloadDeserializer: AvroPayloadDeserializer,
    ): CompensationCommandDeserializer<ProjectCompensationCommand> =
        AvroProjectCompensationCommandTranslator(
            avroPayloadDeserializer = avroPayloadDeserializer,
            topic = SAGA_COMPENSATION_TOPIC,
        )

    @Bean
    fun projectSagaWatchdog(
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
