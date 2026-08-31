package com.vertyll.veds.project.infrastructure.saga

import com.vertyll.veds.project.infrastructure.IntegrationTestBase
import com.vertyll.veds.shared.messaging.kafka.ProcessedEventGuard
import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEventRepositoryPort
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The inbox claim has to share the handler's fate, or the retry and dead-letter machinery above
 * it means nothing: a claim that survived a failed handler would make every redelivery look like
 * a duplicate, so a transient failure would drop the message on its first attempt.
 */
class ProcessedEventGuardIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var guard: ProcessedEventGuard

    @Autowired
    private lateinit var repository: ProcessedEventRepositoryPort

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    private val group = "project-service:test"

    private fun inTransaction(block: () -> Unit) = TransactionTemplate(transactionManager).executeWithoutResult { block() }

    @Test
    fun `a failed handler leaves no claim, so the redelivery is a retry`() {
        val eventId = UUID.randomUUID().toString()

        runCatching {
            inTransaction {
                assertTrue(guard.claim(eventId, group), "the first delivery must be claimed")
                error("handler failed")
            }
        }

        assertFalse(repository.exists(eventId, group), "a rolled-back handler must not leave the event claimed")
        inTransaction {
            assertTrue(guard.claim(eventId, group), "the redelivery must be processed, not skipped as a duplicate")
        }
    }

    @Test
    fun `a committed handler keeps its claim, so a duplicate is skipped`() {
        val eventId = UUID.randomUUID().toString()

        inTransaction { assertTrue(guard.claim(eventId, group)) }

        assertTrue(repository.exists(eventId, group))
        inTransaction { assertFalse(guard.claim(eventId, group), "a genuine duplicate must still be absorbed") }
    }
}
