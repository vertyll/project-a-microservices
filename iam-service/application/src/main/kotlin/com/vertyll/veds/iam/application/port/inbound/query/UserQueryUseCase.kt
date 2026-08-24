package com.vertyll.veds.iam.application.port.inbound.query

import com.vertyll.veds.iam.application.dto.UserResponse
import com.vertyll.veds.iam.domain.model.PageRequest
import com.vertyll.veds.iam.domain.model.PageResult

interface UserQueryUseCase {
    fun getAllUsers(pageRequest: PageRequest): PageResult<UserResponse>

    fun getUserById(id: Long): UserResponse

    fun getUserByEmail(email: String): UserResponse
}
