package com.vertyll.veds.translation.infrastructure.web.controller

import com.vertyll.veds.translation.application.dto.LanguageResponse
import com.vertyll.veds.translation.application.dto.TranslationSnapshotResponse
import com.vertyll.veds.translation.application.port.inbound.query.TranslationQueryUseCase
import com.vertyll.veds.translation.infrastructure.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/translations")
@Tag(name = "Translations", description = "Public translation catalogue")
internal class TranslationController(
    private val queries: TranslationQueryUseCase,
) {
    private companion object {
        private const val SNAPSHOT_RETRIEVED = "translation.snapshot_retrieved"
        private const val LANGUAGES_RETRIEVED = "translation.languages_retrieved"
        private const val CACHE_MINUTES = 5L
    }

    @GetMapping("/{language}")
    @Operation(summary = "Get every effective translation for one language")
    fun snapshot(
        @PathVariable language: String,
    ): ResponseEntity<ApiResponse<TranslationSnapshotResponse>> {
        val snapshot = queries.snapshot(language)
        val body = ApiResponse.buildResponse(snapshot, SNAPSHOT_RETRIEVED, HttpStatus.OK).body

        return ResponseEntity
            .ok()
            .eTag("\"${snapshot.version}\"")
            .cacheControl(CacheControl.maxAge(CACHE_MINUTES, TimeUnit.MINUTES).cachePublic())
            .body(body)
    }

    @GetMapping("/languages")
    @Operation(summary = "List the languages the application offers")
    fun languages(): ResponseEntity<ApiResponse<List<LanguageResponse>>> =
        ApiResponse.buildResponse(queries.languages(), LANGUAGES_RETRIEVED, HttpStatus.OK)
}
