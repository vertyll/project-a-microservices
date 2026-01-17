package com.vertyll.projecta.role.domain.repository

import com.vertyll.projecta.role.domain.model.entity.Role
import com.vertyll.projecta.sharedinfrastructure.role.RoleType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RoleRepository : JpaRepository<Role, Long> {
    fun findByName(name: RoleType): Optional<Role>

    fun existsByName(name: RoleType): Boolean
}
