package com.kornienko.subscriptionsservice.api

import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import java.time.OffsetDateTime
import java.util.*

data class SubscriptionFilter(
    val userId: UUID? = null,
    val serviceName: String? = null,
    val status: SubscriptionStatus? = null,
    val startDateFrom: OffsetDateTime? = null,
    val startDateTo: OffsetDateTime? = null,
    val endDateFrom: OffsetDateTime? = null,
    val endDateTo: OffsetDateTime? = null
)