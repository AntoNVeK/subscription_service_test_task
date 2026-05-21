package com.kornienko.subscriptionsservice.api

import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.OffsetDateTime

@Schema(description = "DTO для частичного обновления подписки (PATCH)")
data class SubscriptionUpdateRequest(

    @Schema(
        description = "Название сервиса",
        example = "Netflix",
        maxLength = 255
    )
    @field:Size(max = 255, message = "Название сервиса не должно превышать 255 символов")
    val serviceName: String? = null,

    @Schema(
        description = "Описание подписки",
        example = "Премиум тариф с 4K",
        nullable = true
    )
    val description: String? = null,

    @Schema(
        description = "Стоимость подписки",
        example = "15.99",
        minimum = "0.01"
    )
    @field:Positive(message = "Стоимость должна быть положительной")
    val cost: BigDecimal? = null,

    @Schema(
        description = "Статус подписки",
        example = "ACTIVE",
        allowableValues = ["ACTIVE", "PAUSED", "CANCELLED", "EXPIRED"]
    )
    val status: SubscriptionStatus? = null,

    @Schema(
        description = "Новая дата окончания подписки (ISO формат)",
        example = "2026-12-31T23:59:59Z",
        nullable = true
    )
    val endAt: OffsetDateTime? = null
)