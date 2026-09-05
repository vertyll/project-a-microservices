package com.vertyll.veds.mail.infrastructure.mail

import com.vertyll.veds.mail.domain.model.EmailTemplate
import org.junit.jupiter.api.Test
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MailTemplateRenderingTest {
    private val renderer =
        ThymeleafTemplateRendererAdapter(
            TemplateEngine().apply {
                setTemplateResolver(
                    ClassLoaderTemplateResolver().apply {
                        prefix = "templates/"
                        suffix = ".html"
                        templateMode = TemplateMode.HTML
                        characterEncoding = Charsets.UTF_8.name()
                    },
                )
            },
        )

    @Test
    fun `a message carries the shared chrome and its own content`() {
        val html = renderer.render("PROJECT_INVITATION", mapOf("projectName" to "Weryfikacja"))

        assertContains(html, "Project invitation")
        assertContains(html, "You have been invited to join a project on VEDS.")
        assertContains(html, "Weryfikacja")
        assertContains(html, "The VEDS Team")
        assertFalse(html.contains("th:"), "no unresolved Thymeleaf attribute may reach the recipient")
    }

    @Test
    fun `the font stack survives as CSS rather than as escaped markup`() {
        val html = renderer.render("TASK_ASSIGNED", emptyMap())

        assertContains(html, "font-family")
        assertFalse(html.contains("&quot;Inter&quot;"), "an entity inside a style attribute would not parse as CSS")
    }

    @Test
    fun `every template renders`() {
        EmailTemplate.entries.forEach { template ->
            val html = renderer.render(template.templateName, mapOf("projectName" to "P", "roleName" to "R"))
            assertTrue(html.contains("VEDS"), "${template.templateName} rendered nothing recognisable")
        }
    }
}
