package com.vertyll.veds.iam.application.port.inbound.query

import com.vertyll.veds.iam.application.dto.PermissionResponse

@Suppress("kotlin:S6517")
interface PermissionQueryUseCase {
    fun getAllPermissions(): List<PermissionResponse>
}
