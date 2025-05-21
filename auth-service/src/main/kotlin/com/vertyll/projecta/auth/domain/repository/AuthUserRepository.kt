package com.vertyll.projecta.auth.domain.repository

import com.vertyll.projecta.auth.domain.model.AuthUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AuthUserRepository : JpaRepository<AuthUser, Long> {
    fun findByEmail(email: String): Optional<AuthUser>

    fun existsByEmail(email: String): Boolean

    fun findByUserId(userId: Long): AuthUser?
} 