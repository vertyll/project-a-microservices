package com.vertyll.projecta.auth.domain.model

enum class SagaStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}
