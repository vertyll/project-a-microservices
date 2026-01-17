package com.vertyll.projecta.role.domain.dto

import com.vertyll.projecta.sharedinfrastructure.role.RoleType

data class RoleCreateDto(
    val name: RoleType,
    val description: String? = null,
)
