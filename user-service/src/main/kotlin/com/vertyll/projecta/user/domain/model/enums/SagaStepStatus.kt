package com.vertyll.projecta.user.domain.model.enums

enum class SagaStepStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED,
}
