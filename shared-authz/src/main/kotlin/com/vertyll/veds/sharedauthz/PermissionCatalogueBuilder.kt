package com.vertyll.veds.sharedauthz

/** Scopes the permission-catalogue DSL so an outer builder's methods stay out of an inner block. */
@DslMarker
annotation class PermissionDsl

/**
 * Receiver of the [permissions] DSL. Not constructed directly.
 */
@PermissionDsl
class PermissionCatalogueBuilder(
    private val module: String,
) {
    private val definitions = mutableListOf<PermissionDefinition>()
    private val declared = mutableSetOf<String>()
    private val stockRoles = mutableListOf<StockRole>()

    fun permission(
        name: String,
        description: String? = null,
        scope: RoleScope = RoleScope.PROJECT,
    ) {
        require(declared.add(name)) { "duplicate permission '$name' in module '$module'" }
        definitions += PermissionDefinition(name = name, description = description, scope = scope)
    }

    fun stockRole(
        name: String,
        scope: RoleScope,
        vararg permissions: String,
    ) {
        require(stockRoles.none { it.name == name }) { "duplicate stock role '$name' in module '$module'" }
        stockRoles += StockRole(name = name, scope = scope, permissions = permissions.toSet())
    }

    internal fun build(): PermissionCatalogue =
        PermissionCatalogue(
            module = module,
            definitions = definitions.toList(),
            stockRoles = stockRoles.toList(),
        )
}
