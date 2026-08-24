package com.vertyll.veds.sharedinfrastructure.saga.contract

/**
 * Marker for an enum that names the saga types of one bounded context.
 *
 * Lives in `shared-contracts` rather than in `shared-infrastructure` so that an
 * application layer can reference a saga type without putting Spring on its
 * compile classpath. The package name is unchanged, so no import had to move.
 */
interface SagaTypeValue {
    val value: String
}
