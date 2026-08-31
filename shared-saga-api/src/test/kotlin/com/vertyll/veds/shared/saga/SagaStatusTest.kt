package com.vertyll.veds.shared.saga

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `isTerminal` is the one piece of behavior in this module, and three engine operations branch
 * on it: completing, awaiting a response and failing a saga are all no-ops once it is true. A
 * status wrongly classified as terminal would silently drop work; wrongly classified as open
 * would let a finished saga be reopened.
 */
class SagaStatusTest {
    @Test
    fun `a saga that reached an outcome is terminal`() {
        listOf(
            SagaStatus.COMPLETED,
            SagaStatus.FAILED,
            SagaStatus.COMPENSATED,
            SagaStatus.COMPENSATION_FAILED,
        ).forEach { assertTrue(it.isTerminal(), "$it is an outcome") }
    }

    @Test
    fun `a saga still in flight is not terminal`() {
        listOf(
            SagaStatus.STARTED,
            SagaStatus.AWAITING_RESPONSE,
            SagaStatus.COMPENSATING,
        ).forEach { assertFalse(it.isTerminal(), "$it is still in flight") }
    }

    /**
     * Guards the split itself: a status added later is neither terminal nor in-flight by
     * accident, because this count fails until someone classifies it above.
     */
    @Test
    fun `every status is classified`() {
        val classified = SagaStatus.entries.count { it.isTerminal() } + SagaStatus.entries.count { !it.isTerminal() }

        assertEquals(SagaStatus.entries.size, classified)
        assertEquals(7, SagaStatus.entries.size, "a new status needs a decision in the two tests above")
    }
}
