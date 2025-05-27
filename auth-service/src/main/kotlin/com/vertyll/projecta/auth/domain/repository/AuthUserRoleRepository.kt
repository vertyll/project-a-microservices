package com.vertyll.projecta.auth.domain.repository

import com.vertyll.projecta.auth.domain.model.entity.AuthUserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AuthUserRoleRepository : JpaRepository<AuthUserRole, Long> {
    fun findByAuthUserId(authUserId: Long): List<AuthUserRole>

    fun findByRoleId(roleId: Long): List<AuthUserRole>

    fun existsByAuthUserIdAndRoleId(authUserId: Long, roleId: Long): Boolean

    fun findByAuthUserIdAndRoleId(authUserId: Long, roleId: Long): Optional<AuthUserRole>

    @Modifying
    @Query("DELETE FROM AuthUserRole ur WHERE ur.authUserId = ?1 AND ur.roleId = ?2")
    fun deleteByAuthUserIdAndRoleId(authUserId: Long, roleId: Long)
}
