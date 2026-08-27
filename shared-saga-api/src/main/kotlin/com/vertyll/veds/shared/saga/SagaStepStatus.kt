package com.vertyll.veds.shared.saga

/**
 * Outcome of a single saga step.
 *
 * Recorded per step rather than only per saga, because compensation has to know
 * *which* steps actually took effect — undoing one that never ran is how a
 * compensation causes the damage it was meant to repair.
 */
enum class SagaStepStatus {
    STARTED,

    COMPLETED,

    FAILED,

    COMPENSATED,

    COMPENSATION_FAILED,

    /**
     * The step did part of its work before failing, so compensation must be
     * written to tolerate a half-applied effect.
     */
    PARTIALLY_COMPLETED,
}
