package com.vertyll.projecta.template.infrastructure.config

import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaOutbox
import com.vertyll.projecta.template.domain.model.entity.Saga
import com.vertyll.projecta.template.domain.model.entity.SagaStep
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EntityScan(basePackageClasses = [Saga::class, SagaStep::class, KafkaOutbox::class])
@EnableScheduling // Enable scheduling for outbox processing
class SagaConfig
