package com.vertyll.veds.template.infrastructure.web.controller

import com.vertyll.veds.template.application.dto.TemplateResponse
import com.vertyll.veds.template.application.port.inbound.command.TemplateCommandUseCase
import com.vertyll.veds.template.infrastructure.response.ApiResponse
import com.vertyll.veds.template.infrastructure.web.dto.CreateTemplateRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/template")
internal class TemplateController(
    private val templateCommands: TemplateCommandUseCase,
) {
    @PostMapping
    fun create(
        @Valid @RequestBody
        request: CreateTemplateRequest,
    ): ResponseEntity<ApiResponse<TemplateResponse>> {
        val template = templateCommands.processTemplateWithSaga(request.toCommand())
        return ApiResponse.buildResponse(
            data = TemplateResponse.from(template),
            message = "Template processed successfully",
            status = HttpStatus.CREATED,
        )
    }
}
