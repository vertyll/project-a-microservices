package com.vertyll.projecta.auth.infrastructure.service

import com.vertyll.projecta.auth.domain.repository.AuthUserRepository
import com.vertyll.projecta.common.exception.ApiException
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val authUserRepository: AuthUserRepository
) : UserDetailsService {

    /**
     * Load user by username (email).
     */
    override fun loadUserByUsername(username: String): UserDetails {
        return authUserRepository.findByEmail(username)
            .orElseThrow { ApiException("User not found", HttpStatus.NOT_FOUND) }
    }
}
