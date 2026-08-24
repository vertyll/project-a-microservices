package com.vertyll.veds.project.application.saga.model

import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaTypeValue

enum class SagaTypes(
    override val value: String,
) : SagaTypeValue {
    PROJECT_INVITATION("ProjectInvitation"),
    PROJECT_ARCHIVAL("ProjectArchival"),
    ;

    companion object {
        fun fromString(value: String): SagaTypes? = entries.find { it.value == value }
    }
}
