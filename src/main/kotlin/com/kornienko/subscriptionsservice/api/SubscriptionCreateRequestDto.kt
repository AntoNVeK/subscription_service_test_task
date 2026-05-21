package com.kornienko.subscriptionsservice.api

import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SubscriptionCreateRequestDto (

    @field:NotNull
    val userId: UUID,

    @field:NotBlank
    val serviceName: String,

    val description: String? = null,

    @field:NotNull
    @field:Positive
    val cost: BigDecimal,

    val startAt: OffsetDateTime? = null,

    val durationMonths: Long? = 1

)