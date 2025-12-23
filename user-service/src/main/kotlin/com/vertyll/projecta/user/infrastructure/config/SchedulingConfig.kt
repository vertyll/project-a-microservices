package com.vertyll.projecta.user.infrastructure.config

import com.vertyll.projecta.user.domain.model.enums.SagaStatus
import com.vertyll.projecta.user.domain.repository.SagaRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Configuration for scheduled tasks in the user service
 */
@Configuration
@EnableScheduling
class SchedulingConfig(
    private val sagaRepository: SagaRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Clean up old sagas that are completed or compensated
     * Runs daily at 2:30 AM
     */
    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    fun cleanupOldSagas() {
        val cutoffDate = Instant.now().minus(30, ChronoUnit.DAYS)

        logger.info("Cleaning up sagas completed before {}", cutoffDate)

        val statuses = listOf(SagaStatus.COMPLETED, SagaStatus.COMPENSATED)
        val oldSagas = sagaRepository.findByStatusInAndStartedAtBefore(statuses, cutoffDate)

        if (oldSagas.isNotEmpty()) {
            logger.info("Found {} old sagas to clean up", oldSagas.size)
            sagaRepository.deleteAll(oldSagas)
            logger.info("Successfully cleaned up {} old sagas", oldSagas.size)
        } else {
            logger.info("No old sagas found to clean up")
        }
    }

    /**
     * Check for stuck sagas and log them
     * Runs every hour at 25 minutes past the hour
     */
    @Scheduled(cron = "0 25 * * * ?")
    @Transactional(readOnly = true)
    fun checkForStuckSagas() {
        val cutoffDate = Instant.now().minus(24, ChronoUnit.HOURS)

        logger.info("Checking for stuck sagas started before {}", cutoffDate)

        val stuckSagas =
            sagaRepository.findByStatusInAndStartedAtBefore(
                listOf(SagaStatus.STARTED, SagaStatus.COMPENSATING),
                cutoffDate,
            )

        if (stuckSagas.isNotEmpty()) {
            logger.warn("Found {} potentially stuck sagas:", stuckSagas.size)
            stuckSagas.forEach { saga ->
                logger.warn(
                    "Stuck saga: ID={}, Type={}, Status={}, StartedAt={}",
                    saga.id,
                    saga.type,
                    saga.status,
                    saga.startedAt,
                )
            }
        } else {
            logger.info("No stuck sagas found")
        }
    }
}
