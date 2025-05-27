package com.vertyll.projecta.role.domain.repository

import com.vertyll.projecta.role.domain.model.entity.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRoleRepository : JpaRepository<UserRole, Long> {
    fun findByUserId(userId: Long): List<UserRole>

    fun findByRoleId(roleId: Long): List<UserRole>

    fun existsByUserIdAndRoleId(userId: Long, roleId: Long): Boolean

    fun findByUserIdAndRoleId(userId: Long, roleId: Long): Optional<UserRole>

    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.userId = ?1 AND ur.roleId = ?2")
    fun deleteByUserIdAndRoleId(userId: Long, roleId: Long)
}
