package com.vertyll.veds.shared.saga

import java.time.Instant

/**
 * A read-only copy of a saga's state, handed to the application layer.
 *
 * The live aggregate is [Saga], implemented by whatever the storage adapter
 * persists. An application service is told what a saga *is* right now and never
 * transitions it directly — every transition goes through [SagaProcessPort], so
 * the engine stays the only writer and a caller cannot leave a saga in a state
 * the engine did not put it in.
 */
data class SagaSnapshot(
    val id: String,
    val type: String,
    val status: SagaStatus,
    val payload: String,
    val lastError: String? = null,
    val startedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
)
