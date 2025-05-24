package com.vertyll.projecta.role.domain.model

enum class SagaStepStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED
}
