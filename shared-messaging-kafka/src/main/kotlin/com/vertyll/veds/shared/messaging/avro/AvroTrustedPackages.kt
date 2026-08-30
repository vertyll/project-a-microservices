package com.vertyll.veds.shared.messaging.avro

import org.apache.avro.util.ClassSecurityValidator

/**
 * Trusts this project's generated Avro classes with Avro's class security validator.
 *
 * Avro 1.12 refuses to resolve a `SpecificRecord` class by name unless it is explicitly
 * trusted, so publishing any event fails with
 * `SecurityException: Forbidden com.vertyll.veds.… This class is not trusted`.
 *
 * The predicate is registered here rather than through the
 * `org.apache.avro.SERIALIZABLE_PACKAGES` system property so that it does not depend on
 * being set before Avro's own classes are loaded, and so that every service inherits it
 * from the module that owns Avro serialization instead of repeating a JVM flag in its
 * run configuration, its tests and its Dockerfile.
 */
internal object AvroTrustedPackages {
    private const val TRUSTED_PREFIX = "com.vertyll.veds."

    private var registered = false

    @Synchronized
    fun register() {
        if (registered) return
        ClassSecurityValidator.setGlobal(
            ClassSecurityValidator.composite(
                ClassSecurityValidator.DEFAULT,
                { it.name.startsWith(TRUSTED_PREFIX) },
            ),
        )
        registered = true
    }
}
