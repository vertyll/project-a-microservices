package com.vertyll.projecta.role.domain.model.enums

enum class SagaStepStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED,
}
