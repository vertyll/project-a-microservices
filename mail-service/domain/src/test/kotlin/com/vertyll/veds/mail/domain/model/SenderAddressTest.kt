package com.vertyll.veds.mail.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The sender comes from configuration. A blank one produces mail that most servers drop without
 * telling anyone, so it is refused at start-up rather than discovered as silent non-delivery.
 */
class SenderAddressTest {
    @Test
    fun `a configured address is kept as given`() {
        assertEquals("no-reply@veds.local", SenderAddress("no-reply@veds.local").value)
    }

    @Test
    fun `a blank address is refused`() {
        listOf("", " ", "\t").forEach { blank ->
            assertFailsWith<IllegalArgumentException> { SenderAddress(blank) }
        }
    }
}
