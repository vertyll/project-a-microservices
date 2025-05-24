package com.vertyll.projecta.user.domain.model

enum class SagaStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}
