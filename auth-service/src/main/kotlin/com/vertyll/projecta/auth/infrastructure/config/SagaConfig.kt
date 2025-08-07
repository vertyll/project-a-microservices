package com.vertyll.projecta.auth.infrastructure.config

import com.vertyll.projecta.auth.domain.model.entity.Saga
import com.vertyll.projecta.auth.domain.model.entity.SagaStep
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaOutbox
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EntityScan(basePackageClasses = [Saga::class, SagaStep::class, KafkaOutbox::class])
@EnableScheduling // Enable scheduling for outbox processing
class SagaConfig
