package com.vertyll.veds.shared.web.error

import com.vertyll.veds.sharederror.ErrorKind
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.json.ProblemDetailJacksonMixin
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The wire shape of a refusal is a contract with every client, so it is pinned here
 * rather than left to whichever service happens to be read first.
 */
class ProblemsTest {
    @Test
    fun `a refusal names the problem by its catalogue key`() {
        val problem = Problems.of(HttpStatus.NOT_FOUND, "task.not_found", instance = "/tasks/42")

        assertEquals(404, problem.status)
        assertEquals("urn:veds:error:task.not_found", problem.type.toString())
        assertEquals("Not Found", problem.title)
        assertEquals("/tasks/42", problem.instance.toString())
        assertEquals("task.not_found", problem.properties?.get("code"))
    }

    /** The prose lives in translation-service, so `detail` would only ever be a key pretending to be a sentence. */
    @Test
    fun `a refusal carries no detail`() {
        assertNull(Problems.of(HttpStatus.NOT_FOUND, "task.not_found").detail)
    }

    @Test
    fun `extension members ride along, and none are invented when there are none`() {
        val withParams =
            Problems.of(
                HttpStatus.NOT_FOUND,
                "task.not_found",
                properties = mapOf("params" to mapOf("id" to "42")),
            )
        assertEquals(mapOf("id" to "42"), withParams.properties?.get("params"))

        assertNull(Problems.of(HttpStatus.NOT_FOUND, "task.not_found").properties?.get("params"))
    }

    @Test
    fun `an instance is omitted rather than guessed when the caller has none`() {
        assertNull(Problems.of(HttpStatus.NOT_FOUND, "task.not_found").instance)
    }
}

/** A kind is transport vocabulary; this is the whole of what it decides. */
class ErrorHttpStatusMapperTest {
    @Test
    fun `every kind maps to the status that says what happened`() {
        val expected =
            mapOf(
                ErrorKind.NOT_FOUND to HttpStatus.NOT_FOUND,
                ErrorKind.UNAUTHENTICATED to HttpStatus.UNAUTHORIZED,
                ErrorKind.ACCESS_DENIED to HttpStatus.FORBIDDEN,
                ErrorKind.CONFLICT to HttpStatus.CONFLICT,
                ErrorKind.INVALID to HttpStatus.BAD_REQUEST,
                ErrorKind.PRECONDITION_FAILED to HttpStatus.PRECONDITION_FAILED,
                ErrorKind.GONE to HttpStatus.GONE,
                ErrorKind.MISCONFIGURED to HttpStatus.INTERNAL_SERVER_ERROR,
            )

        assertEquals(ErrorKind.entries.toSet(), expected.keys, "a new kind needs a status, not a default")
        expected.forEach { (kind, status) -> assertEquals(status, ErrorHttpStatusMapper.toStatus(kind)) }
    }
}

/**
 * RFC 9457 puts extension members beside `type` and `status`, never inside a holder.
 *
 * They land there only because Spring registers [ProblemDetailJacksonMixin] on the
 * mapper — a plain `ObjectMapper` nests them under `properties` and emits a null
 * `detail`, which is not a problem document. Anything serialising a
 * [org.springframework.http.ProblemDetail] by hand, as the gateway's authentication
 * entry point does, has to use the container's mapper for the same reason.
 */
class ProblemSerialisationTest {
    private fun springMapper() =
        JsonMapper
            .builder()
            .addMixIn(ProblemDetail::class.java, ProblemDetailJacksonMixin::class.java)
            .build()

    @Test
    fun `extension members are serialised beside the standard ones`() {
        val json =
            springMapper().writeValueAsString(
                Problems.of(
                    HttpStatus.BAD_REQUEST,
                    "common.validation_failed",
                    instance = "/tasks",
                    properties = mapOf("fields" to mapOf("name" to "common.invalid_value")),
                ),
            )

        val parsed: Map<String, Any> = springMapper().readValue(json, object : TypeReference<Map<String, Any>>() {})

        assertEquals("urn:veds:error:common.validation_failed", parsed["type"])
        assertEquals("Bad Request", parsed["title"])
        assertEquals(400, parsed["status"])
        assertEquals("/tasks", parsed["instance"])
        assertEquals("common.validation_failed", parsed["code"])
        assertEquals(mapOf("name" to "common.invalid_value"), parsed["fields"])
        assertNull(parsed["properties"], "extensions must not nest under a holder")
        assertNull(parsed["detail"])
    }
}
