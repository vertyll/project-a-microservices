package com.vertyll.veds.shared.web.http

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ETagUtilsTest {
    @Test
    fun `builds a weak ETag from a version`() {
        assertEquals("""W/"42"""", ETagUtils.buildWeakETag(42))
    }

    @Test
    fun `propagates no version as no ETag`() {
        assertNull(ETagUtils.buildWeakETag(null))
    }

    @Test
    fun `parses the weak form it produces`() {
        assertEquals(42L, ETagUtils.parseIfMatchToVersion("""W/"42""""))
    }

    @Test
    fun `parses the strong form a client may send instead`() {
        assertEquals(42L, ETagUtils.parseIfMatchToVersion(""""42""""))
    }

    @Test
    fun `parses a bare numeric value`() {
        assertEquals(42L, ETagUtils.parseIfMatchToVersion("42"))
    }

    @Test
    fun `tolerates surrounding whitespace`() {
        assertEquals(42L, ETagUtils.parseIfMatchToVersion("""  W/"42"  """))
    }

    /**
     * An unparseable header yields `null`, which callers treat as "no expected version" — the
     * write then proceeds unguarded. That is the reason the parser is deliberately permissive
     * about form but strict about content.
     */
    @Test
    fun `yields no version for a header that is not a version`() {
        assertNull(ETagUtils.parseIfMatchToVersion("""W/"not-a-number""""))
        assertNull(ETagUtils.parseIfMatchToVersion(""))
        assertNull(ETagUtils.parseIfMatchToVersion("   "))
        assertNull(ETagUtils.parseIfMatchToVersion(null))
    }
}
