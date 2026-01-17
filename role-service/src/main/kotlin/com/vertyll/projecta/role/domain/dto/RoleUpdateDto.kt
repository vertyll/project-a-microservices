package com.vertyll.projecta.role.domain.dto

import com.vertyll.projecta.sharedinfrastructure.role.RoleType

data class RoleUpdateDto(
    val name: RoleType,
    val description: String? = null,
)
