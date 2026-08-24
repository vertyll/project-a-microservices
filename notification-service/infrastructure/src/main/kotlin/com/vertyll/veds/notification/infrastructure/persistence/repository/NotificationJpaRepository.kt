package com.vertyll.veds.notification.infrastructure.persistence.repository

import com.vertyll.veds.notification.infrastructure.persistence.entity.NotificationJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

internal interface NotificationJpaRepository : JpaRepository<NotificationJpaEntity, String>
