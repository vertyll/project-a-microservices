package com.vertyll.projecta.mail.domain.model

enum class SagaStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    PARTIALLY_COMPLETED
}
