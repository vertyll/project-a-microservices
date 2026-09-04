package com.vertyll.veds.mail.domain.error

import com.vertyll.veds.sharederror.DomainError
import com.vertyll.veds.sharederror.ErrorKind

enum class MailError(
    override val key: String,
    override val kind: ErrorKind,
) : DomainError {
    TEMPLATE_NOT_FOUND("mail.template.not_found", ErrorKind.NOT_FOUND),
    TEMPLATE_UNKNOWN("mail.template.unknown", ErrorKind.INVALID),
    TEMPLATE_RENDER_FAILED("mail.template.render_failed", ErrorKind.MISCONFIGURED),
    DELIVERY_FAILED("mail.delivery.failed", ErrorKind.MISCONFIGURED),
    EMAIL_LOG_NOT_FOUND("mail.log.not_found", ErrorKind.NOT_FOUND),
    BATCH_EMPTY("mail.batch.empty", ErrorKind.INVALID),

    VERSION_MISMATCH("common.version_mismatch", ErrorKind.PRECONDITION_FAILED),
}
