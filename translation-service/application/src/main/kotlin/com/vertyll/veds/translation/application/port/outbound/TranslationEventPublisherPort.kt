package com.vertyll.veds.translation.application.port.outbound

interface TranslationEventPublisherPort {
    fun publishTranslationProcessed(
        sagaId: String,
        translationId: Long,
        payload: Map<String, Any?> = emptyMap(),
    )

    fun publishTranslationFailed(
        sagaId: String,
        translationId: Long?,
        error: String,
    )
}
