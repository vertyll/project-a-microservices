package com.vertyll.projecta.mail.domain.model.enums

enum class SagaStepStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED,
    PARTIALLY_COMPLETED
}
