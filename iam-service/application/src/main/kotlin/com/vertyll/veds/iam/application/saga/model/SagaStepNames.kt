package com.vertyll.veds.iam.application.saga.model

import com.vertyll.veds.shared.saga.SagaTypeValue

enum class SagaStepNames(
    override val value: String,
) : SagaTypeValue {
    CREATE_USER("CreateUser"),
    PUBLISH_USER_REGISTERED_EVENT("PublishUserRegisteredEvent"),
    CREATE_VERIFICATION_TOKEN("CreateVerificationToken"),
    REQUEST_MAIL("RequestMail"),
    CREATE_RESET_TOKEN("CreateResetToken"),
    VERIFY_CURRENT_PASSWORD("VerifyCurrentPassword"),
    UPDATE_PASSWORD("UpdatePassword"),
    UPDATE_EMAIL("UpdateEmail"),
    ;

    companion object {
        const val COMPENSATION_PREFIX = "Compensate"
    }
}
