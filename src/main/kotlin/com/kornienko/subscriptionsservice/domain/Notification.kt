package com.kornienko.subscriptionsservice.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "notifications")
class Notification(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "subscription_id", nullable = false)
    val subscriptionId: UUID,

    @Column(name = "message", nullable = false)
    val message: String,

    @Column(name = "scheduled_at", nullable = false)
    val scheduledAt: OffsetDateTime,

    @Column(name = "sent_at")
    var sentAt: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)