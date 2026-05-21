package com.kornienko.subscriptionsservice.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive

@Schema(description = "DTO для продления подписки")
data class SubscriptionRenewRequest(
    @Schema(description = "Количество месяцев для продления", example = "3", minimum = "1", maximum = "120")
    @field:Positive(message = "Количество месяцев должно быть положительным")
    val months: Long = 1
)