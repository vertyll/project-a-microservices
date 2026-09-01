package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.domain.model.Permission
import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.model.RoleType
import com.vertyll.veds.iam.domain.repository.PermissionRepository
import com.vertyll.veds.iam.domain.repository.RoleRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class RoleInitializer(
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
) : ApplicationRunner {
    private companion object {
        private val logger = LoggerFactory.getLogger(RoleInitializer::class.java)

        private val PERMISSIONS =
            mapOf(
                "USERS_VIEW" to "View the user directory",
                "USERS_MANAGE" to "Edit users and assign roles",
                "ROLES_VIEW" to "View roles and their permissions",
                "ROLES_MANAGE" to "Change what a role grants",
                "TRANSLATIONS_VIEW" to "View the translation catalogue",
                "TRANSLATIONS_EDIT" to "Edit translations",
            )

        private val ROLE_PERMISSIONS =
            mapOf(
                RoleType.USER to emptySet(),
                RoleType.ADMIN to PERMISSIONS.keys,
            )

        private val ROLE_DESCRIPTIONS =
            mapOf(
                RoleType.USER to "Standard application user",
                RoleType.ADMIN to "Application administrator",
            )
    }

    @Transactional
    override fun run(args: ApplicationArguments) {
        val permissions = seedPermissions()
        seedRoles(permissions)
    }

    private fun seedPermissions(): Map<String, Permission> =
        PERMISSIONS
            .map { (name, description) ->
                val existing = permissionRepository.findByName(name)
                if (existing != null) {
                    name to existing
                } else {
                    logger.info("Created permission: {}", name)
                    name to permissionRepository.save(Permission.create(name, description))
                }
            }.toMap()

    private fun seedRoles(permissions: Map<String, Permission>) {
        ROLE_PERMISSIONS.forEach { (roleType, permissionNames) ->
            if (roleRepository.existsByName(roleType.value)) {
                logger.debug("Role already exists, leaving its permissions alone: {}", roleType.value)
                return@forEach
            }

            val granted =
                permissionNames
                    .map { name ->
                        permissions[name]
                            ?: error($$"role ${roleType.value} references unknown permission '$name'")
                    }.toSet()

            roleRepository.save(
                Role.create(
                    name = roleType.value,
                    description = ROLE_DESCRIPTIONS[roleType],
                    permissions = granted,
                ),
            )
            logger.info("Created default role {} with {} permissions", roleType.value, granted.size)
        }
    }
}
