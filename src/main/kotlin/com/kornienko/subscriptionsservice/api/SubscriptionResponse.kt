package com.kornienko.subscriptionsservice.api

import com.kornienko.subscriptionsservice.domain.Subscription
import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SubscriptionResponse(

    val id: UUID,
    val userId: UUID,
    val serviceName: String,
    val status: SubscriptionStatus,
    val description: String?,
    val cost: BigDecimal,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
    val version: Long
) {
    companion object {
        fun from(entity: Subscription): SubscriptionResponse {
            return SubscriptionResponse(
                id = entity.id,
                userId = entity.userId,
                serviceName = entity.serviceName,
                status = entity.status,
                description = entity.description,
                cost = entity.cost,
                startAt = entity.startAt!!,
                endAt = entity.endAt!!,
                version = entity.version
            )
        }
    }
}