package com.vertyll.veds.sharedtranslation

import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IcuPatternValidatorTest {
    @Test
    fun `accepts a well-formed pattern`() {
        assertNull(IcuPatternValidator.validate("pl", "Zadanie {name} przypisane"))
    }

    /**
     * The message is the product, not a log line: it is shown to whoever tried to save the
     * pattern, so it has to come back rather than be swallowed.
     */
    @Test
    fun `returns the reason a pattern will not compile`() {
        assertNotNull(IcuPatternValidator.validate("pl", "{count, plural, one{"))
    }

    @Test
    fun `names the offending key when it refuses`() {
        val failure =
            assertFailsWith<IllegalArgumentException> {
                IcuPatternValidator.requireValid("task.count", "pl", "{count, plural, one{")
            }

        assertContains(failure.message.orEmpty(), "task.count")
    }

    @Test
    fun `lists the arguments a pattern expects`() {
        assertEquals(setOf("name", "count"), IcuPatternValidator.argumentsOf("pl", "{name} ma {count} zadań"))
    }

    @Test
    fun `reports no arguments for a pattern that does not compile`() {
        assertEquals(IcuPatternValidator.argumentsOf("pl", "{count, plural, one{"), emptySet())
    }
}
