package com.vertyll.projecta.mail.domain.repository

import com.vertyll.projecta.mail.domain.model.EmailLog
import com.vertyll.projecta.mail.domain.model.EmailLog.EmailStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface EmailLogRepository : JpaRepository<EmailLog, Long> {
    fun findByRecipient(recipient: String): List<EmailLog>

    fun findByTemplateName(templateName: String): List<EmailLog>

    fun findBySentAtBetween(start: Instant, end: Instant): List<EmailLog>

    fun countByStatusAndSentAtBetween(status: EmailStatus, start: Instant, end: Instant): Long
}
