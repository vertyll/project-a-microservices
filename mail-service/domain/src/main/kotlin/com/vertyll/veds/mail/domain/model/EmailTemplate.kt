package com.vertyll.veds.mail.domain.model

enum class EmailTemplate(
    val templateName: String,
) {
    ACTIVATE_ACCOUNT("ACTIVATE_ACCOUNT"),
    WELCOME_EMAIL("WELCOME_EMAIL"),

    RESET_PASSWORD("RESET_PASSWORD"),
    CHANGE_PASSWORD("CHANGE_PASSWORD"),
    SET_NEW_PASSWORD("SET_NEW_PASSWORD"),

    CHANGE_EMAIL("CHANGE_EMAIL"),

    PROJECT_INVITATION("PROJECT_INVITATION"),
    PROJECT_MEMBER_JOINED("PROJECT_MEMBER_JOINED"),
    TASK_CREATED("TASK_CREATED"),
    TASK_ASSIGNED("TASK_ASSIGNED"),
    TASK_STATUS_CHANGED("TASK_STATUS_CHANGED"),
    TASK_COMMENT_ADDED("TASK_COMMENT_ADDED"),
    ;

    companion object {
        fun fromTemplateName(name: String): EmailTemplate? = EmailTemplate.entries.find { it.templateName == name }
    }
}
