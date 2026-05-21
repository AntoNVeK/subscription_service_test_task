package com.kornienko.subscriptionsservice.domain.repository

import com.kornienko.subscriptionsservice.domain.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.*

interface NotificationRepository : JpaRepository<Notification, UUID> {

    @Query(
        value = """
            SELECT * FROM notifications 
            WHERE sent_at IS NULL AND scheduled_at <= :now
            ORDER BY scheduled_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true
    )
    fun findPendingNotificationsForUpdate(
        @Param("now") now: OffsetDateTime,
        @Param("limit") limit: Int
    ): List<Notification>

    @Modifying
    @Query("UPDATE Notification n SET n.sentAt = :now WHERE n.id = :id")
    fun markAsSent(id: UUID, now: OffsetDateTime)

    fun findBySubscriptionId(subscriptionId: UUID): List<Notification>
}