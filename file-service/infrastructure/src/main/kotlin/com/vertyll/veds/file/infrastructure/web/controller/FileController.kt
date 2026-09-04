package com.vertyll.veds.file.infrastructure.web.controller

import com.vertyll.veds.file.application.command.AttachFileCommand
import com.vertyll.veds.file.application.command.ConfirmUploadCommand
import com.vertyll.veds.file.application.dto.DownloadTicketResponse
import com.vertyll.veds.file.application.dto.FileResponse
import com.vertyll.veds.file.application.dto.UploadTicketResponse
import com.vertyll.veds.file.application.port.inbound.command.FileCommandUseCase
import com.vertyll.veds.file.application.port.inbound.query.FileQueryUseCase
import com.vertyll.veds.file.infrastructure.web.dto.AttachFileRequest
import com.vertyll.veds.file.infrastructure.web.dto.RequestUploadRequest
import com.vertyll.veds.file.infrastructure.web.security.CurrentUser
import com.vertyll.veds.shared.web.http.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/files")
@Tag(name = "Files", description = "Upload tickets and file metadata")
internal class FileController(
    private val commands: FileCommandUseCase,
    private val queries: FileQueryUseCase,
) {
    private companion object {
        private const val UPLOAD_REQUESTED = "file.upload_requested"
        private const val UPLOAD_CONFIRMED = "file.upload_confirmed"
        private const val FILE_ATTACHED = "file.attached"
        private const val FILE_RETRIEVED = "file.retrieved"
        private const val FILE_DELETED = "file.deleted"
        private const val DOWNLOAD_READY = "file.download_ready"
    }

    @PostMapping("/upload-ticket")
    @Operation(summary = "Ask for permission to upload and get a signed URL")
    fun requestUpload(
        @AuthenticationPrincipal jwt: Jwt?,
        @Valid @RequestBody
        request: RequestUploadRequest,
    ): ResponseEntity<ApiResponse<UploadTicketResponse>> {
        val ticket = commands.requestUpload(request.toCommand(), CurrentUser.actorOf(jwt))
        return ApiResponse.buildResponse(ticket, UPLOAD_REQUESTED, HttpStatus.CREATED)
    }

    @PostMapping("/{fileId}/confirm")
    @Operation(summary = "Confirm that the upload completed")
    fun confirmUpload(
        @AuthenticationPrincipal jwt: Jwt?,
        @PathVariable fileId: UUID,
    ): ResponseEntity<ApiResponse<FileResponse>> {
        val file = commands.confirmUpload(ConfirmUploadCommand(fileId), CurrentUser.actorOf(jwt))
        return ApiResponse.buildResponse(file, UPLOAD_CONFIRMED, HttpStatus.OK)
    }

    @PostMapping("/{fileId}/attach")
    @Operation(summary = "Bind a confirmed file to the aggregate that owns it")
    fun attach(
        @AuthenticationPrincipal jwt: Jwt?,
        @PathVariable fileId: UUID,
        @Valid @RequestBody
        request: AttachFileRequest,
    ): ResponseEntity<ApiResponse<FileResponse>> {
        val file = commands.attach(AttachFileCommand(fileId, request.scopeId), CurrentUser.actorOf(jwt))
        return ApiResponse.buildResponse(file, FILE_ATTACHED, HttpStatus.OK)
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get a file's metadata")
    fun getFile(
        @AuthenticationPrincipal jwt: Jwt?,
        @PathVariable fileId: UUID,
    ): ResponseEntity<ApiResponse<FileResponse>> =
        ApiResponse.buildResponse(queries.getFile(fileId, CurrentUser.actorOf(jwt)), FILE_RETRIEVED, HttpStatus.OK)

    @GetMapping("/{fileId}/download-ticket")
    @Operation(summary = "Get a short-lived URL to download the file")
    fun requestDownload(
        @AuthenticationPrincipal jwt: Jwt?,
        @PathVariable fileId: UUID,
    ): ResponseEntity<ApiResponse<DownloadTicketResponse>> {
        val ticket = queries.requestDownload(fileId, CurrentUser.actorOf(jwt))
        return ApiResponse.buildResponse(ticket, DOWNLOAD_READY, HttpStatus.OK)
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Delete a file")
    fun delete(
        @AuthenticationPrincipal jwt: Jwt?,
        @PathVariable fileId: UUID,
    ): ResponseEntity<ApiResponse<Any>> {
        commands.delete(fileId, CurrentUser.actorOf(jwt))
        return ApiResponse.buildResponse(null, FILE_DELETED, HttpStatus.OK)
    }
}
