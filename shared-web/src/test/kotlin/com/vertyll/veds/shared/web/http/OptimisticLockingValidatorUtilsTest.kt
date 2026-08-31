package com.vertyll.veds.shared.web.http

import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException
import kotlin.test.assertFailsWith

class OptimisticLockingValidatorUtilsTest {
    @Test
    fun `passes when the client holds the current version`() {
        OptimisticLockingValidatorUtils.validate(currentVersion = 4, expectedVersion = 4)
    }

    @Test
    fun `refuses a write from a client holding a stale version`() {
        assertFailsWith<OptimisticLockingFailureException> {
            OptimisticLockingValidatorUtils.validate(currentVersion = 5, expectedVersion = 4)
        }
    }

    /**
     * A caller that sent no `If-Match` is not asserting anything about the current state, so the
     * write proceeds. Treating a missing header as a mismatch would make every unconditional
     * update fail.
     */
    @Test
    fun `treats no expected version as no check requested`() {
        OptimisticLockingValidatorUtils.validate(currentVersion = 5, expectedVersion = null)
    }

    @Test
    fun `refuses when the row carries no version but one was expected`() {
        assertFailsWith<OptimisticLockingFailureException> {
            OptimisticLockingValidatorUtils.validate(currentVersion = null, expectedVersion = 4)
        }
    }

    @Test
    fun `lets the caller choose the exception it wants`() {
        assertFailsWith<IllegalStateException> {
            OptimisticLockingValidatorUtils.validate(currentVersion = 5, expectedVersion = 4) { IllegalStateException("stale") }
        }
    }
}
