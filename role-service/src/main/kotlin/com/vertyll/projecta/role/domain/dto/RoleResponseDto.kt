package com.vertyll.projecta.role.domain.dto

import com.vertyll.projecta.sharedinfrastructure.role.RoleType

data class RoleResponseDto(
    val id: Long,
    val name: RoleType,
    val description: String?,
)
