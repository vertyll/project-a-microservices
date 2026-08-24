package com.vertyll.veds.iam.application.port.inbound

import com.vertyll.veds.iam.application.command.UpdateProfileCommand
import com.vertyll.veds.iam.application.dto.UserResponse
import com.vertyll.veds.iam.domain.model.PageRequest
import com.vertyll.veds.iam.domain.model.PageResult

interface UserUseCase {
    fun getAllUsers(pageRequest: PageRequest): PageResult<UserResponse>

    fun getUserById(id: Long): UserResponse

    fun getUserByEmail(email: String): UserResponse

    fun updateProfile(
        id: Long,
        request: UpdateProfileCommand,
        version: Long? = null,
    ): UserResponse
}
