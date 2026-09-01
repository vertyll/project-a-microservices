package com.vertyll.veds.mail.application.service

import com.vertyll.veds.mail.application.port.inbound.EmailUseCase
import com.vertyll.veds.mail.domain.model.EmailTemplate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmailBatchServiceTest {
    private val sent = mutableListOf<Pair<String, Map<String, String>>>()
    private var refuse = emptySet<String>()

    private val emails =
        object : EmailUseCase {
            override fun sendEmail(
                to: String,
                subject: String,
                template: EmailTemplate,
                variables: Map<String, String>,
                replyTo: String?,
            ): Boolean {
                sent += to to variables
                return to !in refuse
            }

            override fun getEmailLogs() = error("not used by the batch")
        }

    private val service = EmailBatchService(emails)

    private fun send(
        recipients: List<String>,
        common: Map<String, String> = emptyMap(),
        specific: Map<String, Map<String, String>> = emptyMap(),
    ) = service.processEmailBatch(
        recipients = recipients,
        subject = "Welcome",
        template = EmailTemplate.WELCOME_EMAIL,
        commonVariables = common,
        specificVariables = specific,
        replyTo = null,
    )

    @Test
    fun `every recipient gets one mail`() {
        val results = send(listOf("a@example.com", "b@example.com"))

        assertEquals(listOf("a@example.com", "b@example.com"), sent.map { it.first })
        assertEquals(mapOf("a@example.com" to true, "b@example.com" to true), results)
    }

    @Test
    fun `common variables reach every recipient`() {
        send(listOf("a@example.com", "b@example.com"), common = mapOf("projectName" to "Apollo"))

        assertTrue(sent.all { it.second["projectName"] == "Apollo" })
    }

    @Test
    fun `a recipient's own variables override the common ones`() {
        send(
            listOf("a@example.com", "b@example.com"),
            common = mapOf("firstName" to "there", "projectName" to "Apollo"),
            specific = mapOf("a@example.com" to mapOf("firstName" to "Ada")),
        )

        assertEquals("Ada", sent.first { it.first == "a@example.com" }.second["firstName"])
        assertEquals("there", sent.first { it.first == "b@example.com" }.second["firstName"])
        assertEquals("Apollo", sent.first { it.first == "a@example.com" }.second["projectName"])
    }

    @Test
    fun `a refused recipient does not stop the rest`() {
        refuse = setOf("b@example.com")

        val results = send(listOf("a@example.com", "b@example.com", "c@example.com"))

        assertEquals(3, sent.size)
        assertEquals(mapOf("a@example.com" to true, "b@example.com" to false, "c@example.com" to true), results)
    }

    @Test
    fun `an empty batch sends nothing`() {
        assertTrue(send(emptyList()).isEmpty())
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `personalisation for an absent recipient is ignored`() {
        send(listOf("a@example.com"), specific = mapOf("nobody@example.com" to mapOf("firstName" to "Ghost")))

        assertEquals(1, sent.size)
        assertTrue(sent.single().second.isEmpty())
    }
}
