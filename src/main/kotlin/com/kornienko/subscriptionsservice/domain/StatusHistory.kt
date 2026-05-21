package com.kornienko.subscriptionsservice.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "subscription_status_history")
data class StatusHistory(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "subscription_id", nullable = false)
    val subscriptionId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    val oldStatus: SubscriptionStatus? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    val newStatus: SubscriptionStatus,

    @Column(name = "changed_at", nullable = false)
    val changedAt: OffsetDateTime,

    @Column(name = "changed_by", nullable = false)
    val changedBy: String = "system"
)