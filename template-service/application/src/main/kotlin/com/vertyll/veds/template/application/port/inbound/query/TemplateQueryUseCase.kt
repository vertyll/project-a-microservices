package com.vertyll.veds.template.application.port.inbound.query

import com.vertyll.veds.template.application.dto.TemplateResponse

interface TemplateQueryUseCase {
    fun getById(id: String): TemplateResponse
}
