package com.vertyll.projecta.auth.infrastructure.service

import com.vertyll.projecta.auth.domain.repository.AuthUserRepository
import com.vertyll.projecta.auth.infrastructure.exception.ApiException
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val authUserRepository: AuthUserRepository,
) : UserDetailsService {
    private companion object {
        private const val USER_NOT_FOUND = "User not found"
    }

    /**
     * Load user by username (email).
     */
    override fun loadUserByUsername(username: String): UserDetails =
        authUserRepository
            .findByEmail(username)
            .orElseThrow {
                ApiException(
                    message = USER_NOT_FOUND,
                    status = HttpStatus.NOT_FOUND,
                )
            }
}
