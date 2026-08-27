package com.vertyll.veds.template.application.saga.model

import com.vertyll.veds.shared.saga.SagaTypeValue

/**
 * Saga types for template-service.
 *
 * Replace these placeholder values with your actual business saga types
 * when cloning this service for a new microservice.
 */
enum class SagaTypes(
    override val value: String,
) : SagaTypeValue {
    TEMPLATE_PROCESSING("TemplateProcessing"),
    ;

    companion object {
        fun fromString(value: String): SagaTypes? = SagaTypes.entries.find { it.value == value }
    }
}
