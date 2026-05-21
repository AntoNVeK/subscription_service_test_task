package com.kornienko.subscriptionsservice.api

import com.kornienko.subscriptionsservice.domain.Notification
import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import com.kornienko.subscriptionsservice.domain.service.NotificationService
import com.kornienko.subscriptionsservice.domain.service.SubscriptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Подписки", description = "API для управления подписками")
class SubscriptionController (
    private val subscriptionService: SubscriptionService,
    private val notificationService: NotificationService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/{id}")
    @Operation(summary = "Получить подписку по ID")
    fun getById(@PathVariable id: UUID): SubscriptionResponse {
        return subscriptionService.findById(id)
    }

    @PostMapping
    @Operation(summary = "Создание подписки")
    fun createSubscription(
        @RequestBody @Valid request: SubscriptionCreateRequestDto
    ): SubscriptionResponse {
        return subscriptionService.create(request)
    }

    @GetMapping
    @Operation(summary = "Получить все подписки с фильтрами, пагинацией и сортировкой")
    fun getAll(
        @RequestParam(required = false) userId: UUID?,
        @RequestParam(required = false) serviceName: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) startDateFrom: OffsetDateTime?,
        @RequestParam(required = false) startDateTo: OffsetDateTime?,
        @RequestParam(required = false) endDateFrom: OffsetDateTime?,
        @RequestParam(required = false) endDateTo: OffsetDateTime?,
        pageableRequest: PageableRequest
    ): Page<SubscriptionResponse> {

        val effectivePageable = if (pageableRequest.sort.isNullOrBlank()) {
            PageRequest.of(
                pageableRequest.page,
                pageableRequest.size ?: 20,
                Sort.by(Sort.Direction.DESC, "createdAt")
            )
        } else {
            pageableRequest.toPageable("createdAt", "desc")
        }

        val statusEnum = status?.let {
            try {
                SubscriptionStatus.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Недопустимое значение статуса: $it")
            }
        }

        val filter = SubscriptionFilter(
            userId = userId,
            serviceName = serviceName,
            status = statusEnum,
            startDateFrom = startDateFrom,
            startDateTo = startDateTo,
            endDateFrom = endDateFrom,
            endDateTo = endDateTo
        )

        return subscriptionService.findAll(filter, effectivePageable)
    }

    /**
     * Частичное обновление подписки
     *
     * PATCH /api/v1/subscriptions/{id}
     *
     * Обновляет только те поля, которые переданы в запросе.
     * Поля со значением null игнорируются.
     *
     * @param id Идентификатор подписки
     * @param request DTO с полями для обновления
     * @return Обновлённая подписка
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Частичное обновление подписки")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: SubscriptionUpdateRequest
    ): SubscriptionResponse {
        return subscriptionService.updateSubscription(id, request)
    }

    /**
     * Обновление статуса подписки
     *
     * PATCH /api/v1/subscriptions/{id}/status?status=PAUSED
     *
     * @param id Идентификатор подписки
     * @param status Новый статус
     * @return Обновлённая подписка
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Обновить статус подписки")
    fun updateStatus(
        @PathVariable id: UUID,
        @RequestParam status: SubscriptionStatus
    ): SubscriptionResponse {
        return subscriptionService.updateStatus(id, status)
    }

    /**
     * Отмена подписки
     *
     * POST /api/v1/subscriptions/{id}/cancel
     *
     * @param id Идентификатор подписки
     * @return Отменённая подписка
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Отменить подписку")
    fun cancel(@PathVariable id: UUID): SubscriptionResponse {
        return subscriptionService.cancelSubscription(id)
    }

    /**
     * Приостановка подписки
     *
     * POST /api/v1/subscriptions/{id}/pause
     *
     * @param id Идентификатор подписки
     * @return Приостановленная подписка
     */
    @PostMapping("/{id}/pause")
    @Operation(summary = "Приостановить подписку")
    fun pause(@PathVariable id: UUID): SubscriptionResponse {
        return subscriptionService.pauseSubscription(id)
    }

    /**
     * Возобновление приостановленной подписки
     *
     * POST /api/v1/subscriptions/{id}/resume
     *
     * @param id Идентификатор подписки
     * @return Возобновлённая подписка
     */
    @PostMapping("/{id}/resume")
    @Operation(summary = "Возобновить приостановленную подписку")
    fun resume(@PathVariable id: UUID): SubscriptionResponse {
        return subscriptionService.resumeSubscription(id)
    }


    /**
     * Получение активных подписок пользователя
     *
     * GET /api/v1/subscriptions/users/{userId}/active
     *
     * @param userId Идентификатор пользователя
     * @return Список активных подписок
     */
    @GetMapping("/users/{userId}/active")
    @Operation(summary = "Получить активные подписки пользователя")
    fun getActiveSubscriptions(@PathVariable userId: UUID): List<SubscriptionResponse> {
        return subscriptionService.getActiveSubscriptions(userId)
    }

    @PostMapping("/{id}/renew")
    @Operation(
        summary = "Продлить подписку",
        description = "Продлевает подписку на указанное количество месяцев. " +
                "Если подписка была истекшей или приостановленной, она будет активирована."
    )
    fun renewSubscription(
        @PathVariable id: UUID,
        @RequestBody @Valid request: SubscriptionRenewRequest
    ): SubscriptionResponse {
        return subscriptionService.renewSubscription(id, request.months)
    }


    @GetMapping("/{subscriptionId}/notifications")
    fun getNotifications(@PathVariable subscriptionId: UUID): List<Notification> {
        return notificationService.getBySubscriptionId(subscriptionId)
    }
}