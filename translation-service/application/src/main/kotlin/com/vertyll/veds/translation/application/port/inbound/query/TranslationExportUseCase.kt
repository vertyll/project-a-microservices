package com.vertyll.veds.translation.application.port.inbound.query

import com.vertyll.veds.translation.application.dto.ExportRowResponse

interface TranslationExportUseCase {
    fun exportRows(): List<ExportRowResponse>

    fun exportHeaders(language: String): List<String>
}
