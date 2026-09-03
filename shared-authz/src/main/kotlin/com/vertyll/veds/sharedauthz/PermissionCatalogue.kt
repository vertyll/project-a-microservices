package com.vertyll.veds.sharedauthz

/**
 * The permissions one module enforces, declared by the service that owns them.
 *
 * A permission is real only where some code checks it, so the catalogue is code
 * rather than data: nothing may invent a permission the owning service does not
 * know how to enforce. What *is* data is which permissions a role holds — that
 * belongs to iam-service and is edited by administrators.
 */
data class PermissionCatalogue(
    val module: String,
    val definitions: List<PermissionDefinition>,
    val stockRoles: List<StockRole> = emptyList(),
) {
    init {
        require(module.isNotBlank()) { "a permission catalogue must name its module" }
        require(definitions.isNotEmpty()) { "module '$module' declared no permissions" }

        val byName = definitions.associateBy { it.name }
        stockRoles.forEach { role ->
            val unknown = role.permissions - byName.keys
            require(unknown.isEmpty()) {
                "stock role '${role.name}' of module '$module' grants permissions this module does not declare: $unknown"
            }

            val wrongScope = role.permissions.filter { byName.getValue(it).scope != role.scope }
            require(wrongScope.isEmpty()) {
                "stock role '${role.name}' is ${role.scope} but grants permissions held in another scope: $wrongScope"
            }
        }
    }

    val names: Set<String> get() = definitions.mapTo(linkedSetOf()) { it.name }
}

/**
 * A role the module ships with, and what it starts out granting.
 *
 * The grant applies only where iam-service has no such role yet. Once the role
 * exists an administrator owns it, so redeploying a module never silently
 * restores permissions somebody deliberately took away.
 */
data class StockRole(
    val name: String,
    val scope: RoleScope,
    val permissions: Set<String>,
) {
    init {
        require(name.matches(ROLE_NAME_PATTERN)) { "role name '$name' must be UPPER_SNAKE_CASE" }
    }

    private companion object {
        private val ROLE_NAME_PATTERN = Regex("^[A-Z][A-Z0-9_]*$")
    }
}

/** Where a role is held: across the platform, or inside one project. */
enum class RoleScope {
    GLOBAL,
    PROJECT,
}

/**
 * @property scope where the permission can be held. A permission enforced against
 *           a project membership is meaningless on a platform-wide role, and one
 *           enforced against the platform is meaningless inside a project, so the
 *           module that knows the difference states it and nothing can mix them.
 */
data class PermissionDefinition(
    val name: String,
    val description: String? = null,
    val scope: RoleScope,
) {
    init {
        require(name.matches(NAME_PATTERN)) { "permission name '$name' must be UPPER_SNAKE_CASE" }
    }

    private companion object {
        private val NAME_PATTERN = Regex("^[A-Z][A-Z0-9_]*$")
    }
}

/** Declares the permissions a service enforces. */
fun permissions(
    module: String,
    block: PermissionCatalogueBuilder.() -> Unit,
): PermissionCatalogue = PermissionCatalogueBuilder(module).apply(block).build()
