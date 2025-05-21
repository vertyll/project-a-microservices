package com.vertyll.projecta.role.domain.repository

import com.vertyll.projecta.role.domain.model.Role
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RoleRepository : JpaRepository<Role, Long> {
    fun findByName(name: String): Optional<Role>

    fun existsByName(name: String): Boolean
}
