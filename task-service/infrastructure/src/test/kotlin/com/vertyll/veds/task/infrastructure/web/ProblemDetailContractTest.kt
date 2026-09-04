package com.vertyll.veds.task.infrastructure.web

import com.vertyll.veds.task.infrastructure.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * What a caller actually receives, over the real HTTP stack and the container's own
 * `ObjectMapper`.
 *
 * The unit test in `shared-web` pins how the document is built; this pins that Spring
 * still serializes it that way once the whole chain — advice, message converter,
 * Jackson mixin — is in play. That chain is exactly where a problem document quietly
 * turns back into `{ "properties": { … } }` on an upgrade.
 */
@AutoConfigureMockMvc
internal class ProblemDetailContractTest(
    private val mockMvc: MockMvc,
) : IntegrationTestBase() {
    private companion object {
        private const val LANGUAGE_HEADER = "X-Lang"
    }

    private fun caller() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("email", "caller@example.com") }

    @Test
    fun `a refusal is an RFC 9457 problem document naming the catalogue key`() {
        mockMvc
            .perform(get("/tasks/{id}", UUID.randomUUID()).header(LANGUAGE_HEADER, "pl").with(caller()))
            .andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:veds:error:task.not_found"))
            .andExpect(jsonPath("$.title").value("Not Found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("task.not_found"))
            .andExpect(jsonPath("$.instance").exists())
            .andExpect(jsonPath("$.detail").doesNotExist())
            .andExpect(jsonPath("$.properties").doesNotExist())
    }

    @Test
    fun `a validation refusal names the fields that were rejected`() {
        mockMvc
            .perform(
                post("/tasks/project/{projectId}", UUID.randomUUID())
                    .with(caller())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"","priceEstimation":-1}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("common.validation_failed"))
            .andExpect(jsonPath("$.fields.name").value("validation.task.description_required"))
            .andExpect(jsonPath("$.fields.priceEstimation").value("validation.task.estimation_negative"))
    }

    /**
     * Spring answers an unauthenticated request with `WWW-Authenticate` and an empty
     * body unless told otherwise. A caller that reads `code` would then have to
     * special-case one status, so the entry point fills the body in.
     */
    @Test
    fun `an unauthenticated caller is refused in the same shape`() {
        mockMvc
            .perform(get("/tasks/{id}", UUID.randomUUID()).header(LANGUAGE_HEADER, "pl"))
            .andExpect(status().isUnauthorized)
            .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("common.not_authenticated"))
            .andExpect(jsonPath("$.properties").doesNotExist())
    }

    /**
     * The catch-all advice runs first so the handlers above win, which puts Spring's own
     * failures through it too. An unknown path is a `404`; turning it into a `500` would
     * report a defect in the service for a request the caller got wrong.
     */
    @Test
    fun `a path nothing serves is a 404, not a 500`() {
        mockMvc
            .perform(get("/tasks/no-such-endpoint/at-all").header(LANGUAGE_HEADER, "pl").with(caller()))
            .andExpect(status().isNotFound)
    }

    /** `fields` is absent rather than empty, the way `params` is: an empty member says nothing. */
    @Test
    fun `an unreadable body is refused without inventing an empty field list`() {
        mockMvc
            .perform(
                post("/tasks/project/{projectId}", UUID.randomUUID())
                    .with(caller())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("common.validation_failed"))
            .andExpect(jsonPath("$.fields").doesNotExist())
    }

    /** A success carries the payload and nothing around it — no `data`, no `message`, no `timestamp`. */
    @Test
    fun `a success is the payload itself`() {
        mockMvc
            .perform(get("/tasks/project/{projectId}/permissions", UUID.randomUUID()).with(caller()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.message").doesNotExist())
            .andExpect(jsonPath("$.timestamp").doesNotExist())
    }
}
