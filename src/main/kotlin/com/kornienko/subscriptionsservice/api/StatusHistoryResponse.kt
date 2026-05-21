package com.kornienko.subscriptionsservice.api

import com.kornienko.subscriptionsservice.domain.StatusHistory
import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import java.time.OffsetDateTime
import java.util.*

data class StatusHistoryResponse(
    val id: UUID,
    val subscriptionId: UUID,
    val oldStatus: SubscriptionStatus?,
    val newStatus: SubscriptionStatus,
    val changedAt: OffsetDateTime
) {
    companion object {
        fun from(history: StatusHistory): StatusHistoryResponse {
            return StatusHistoryResponse(
                id = history.id,
                subscriptionId = history.subscriptionId,
                oldStatus = history.oldStatus,
                newStatus = history.newStatus,
                changedAt = history.changedAt
            )
        }
    }
}