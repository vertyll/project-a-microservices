package com.vertyll.projecta.user.domain.repository

import com.vertyll.projecta.user.domain.model.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>

    fun existsByEmail(email: String): Boolean

    @Query("SELECT u FROM User u WHERE u.email = :email")
    fun findByEmailWithRoles(email: String): Optional<User>
}
