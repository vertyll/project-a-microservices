package com.vertyll.projecta.mail.infrastructure.config

import com.vertyll.projecta.common.kafka.KafkaOutbox
import com.vertyll.projecta.mail.domain.model.entity.Saga
import com.vertyll.projecta.mail.domain.model.entity.SagaStep
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EntityScan(basePackageClasses = [Saga::class, SagaStep::class, KafkaOutbox::class])
@EnableScheduling // Enable scheduling for outbox processing
class SagaConfig
