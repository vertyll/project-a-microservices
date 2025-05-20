package com.vertyll.projecta.mail.infrastructure.config

import com.vertyll.projecta.common.kafka.KafkaOutbox
import com.vertyll.projecta.common.kafka.KafkaOutboxRepository
import com.vertyll.projecta.common.saga.Saga
import com.vertyll.projecta.common.saga.SagaRepository
import com.vertyll.projecta.common.saga.SagaStep
import com.vertyll.projecta.common.saga.SagaStepRepository
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EntityScan(basePackageClasses = [Saga::class, SagaStep::class, KafkaOutbox::class])
@EnableScheduling // Enable scheduling for outbox processing
class SagaConfig
