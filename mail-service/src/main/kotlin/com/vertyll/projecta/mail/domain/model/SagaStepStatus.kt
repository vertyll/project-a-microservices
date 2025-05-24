package com.vertyll.projecta.mail.domain.model

enum class SagaStepStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED,
    PARTIALLY_COMPLETED
}
