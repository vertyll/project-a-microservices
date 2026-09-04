package com.vertyll.veds.template.domain.error

import com.vertyll.veds.sharederror.DomainError
import com.vertyll.veds.sharederror.ErrorKind

enum class TemplateError(
    override val key: String,
    override val kind: ErrorKind,
) : DomainError {
    NOT_FOUND("template.not_found", ErrorKind.NOT_FOUND),
    ACCESS_DENIED("template.access_denied", ErrorKind.ACCESS_DENIED),
    ALREADY_EXISTS("template.already_exists", ErrorKind.CONFLICT),
    MISCONFIGURED("template.misconfigured", ErrorKind.MISCONFIGURED),

    VERSION_MISMATCH("common.version_mismatch", ErrorKind.PRECONDITION_FAILED),
}
