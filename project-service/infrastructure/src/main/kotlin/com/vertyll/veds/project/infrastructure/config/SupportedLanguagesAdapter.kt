package com.vertyll.veds.project.infrastructure.config

import com.vertyll.veds.project.application.port.outbound.SupportedLanguagesPort
import com.vertyll.veds.project.domain.model.LanguageTag
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
internal class SupportedLanguagesAdapter(
    @Value("\${veds.translation.supported-languages}") private val configured: List<String>,
) : SupportedLanguagesPort {
    private val languages: Set<LanguageTag> =
        configured
            .map { LanguageTag.of(it) }
            .toSet()
            .also { check(it.isNotEmpty()) { "veds.translation.supported-languages must not be empty" } }

    override fun supported(): Set<LanguageTag> = languages
}
