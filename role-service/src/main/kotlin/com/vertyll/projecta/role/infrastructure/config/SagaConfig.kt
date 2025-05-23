package com.vertyll.projecta.role.infrastructure.config

import com.vertyll.projecta.common.kafka.KafkaOutbox
import com.vertyll.projecta.role.domain.model.Saga
import com.vertyll.projecta.role.domain.model.SagaStep
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EntityScan(basePackageClasses = [Saga::class, SagaStep::class, KafkaOutbox::class])
@EnableScheduling // Enable scheduling for outbox processing
class SagaConfig
