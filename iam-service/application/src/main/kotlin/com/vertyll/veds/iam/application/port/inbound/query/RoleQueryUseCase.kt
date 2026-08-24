package com.vertyll.veds.iam.application.port.inbound.query

import com.vertyll.veds.iam.application.dto.RoleResponse

interface RoleQueryUseCase {
    fun getRoleById(id: Long): RoleResponse

    fun getRoleByName(name: String): RoleResponse

    fun getAllRoles(): List<RoleResponse>

    fun getRolesForUser(userId: Long): List<RoleResponse>
}
