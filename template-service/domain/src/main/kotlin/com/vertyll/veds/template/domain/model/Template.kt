package com.vertyll.veds.template.domain.model

import java.time.Instant
import java.util.UUID

data class Template(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val payload: String,
    val status: TemplateStatus = TemplateStatus.CREATED,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    fun markProcessed(): Template = copy(status = TemplateStatus.PROCESSED, updatedAt = Instant.now())

    fun markFailed(): Template = copy(status = TemplateStatus.FAILED, updatedAt = Instant.now())
}
