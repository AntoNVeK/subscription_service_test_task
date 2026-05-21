package com.kornienko.subscriptionsservice.domain.service

import com.kornienko.subscriptionsservice.api.SubscriptionCreateRequestDto
import com.kornienko.subscriptionsservice.api.SubscriptionFilter
import com.kornienko.subscriptionsservice.api.SubscriptionResponse
import com.kornienko.subscriptionsservice.api.SubscriptionUpdateRequest
import com.kornienko.subscriptionsservice.domain.Subscription
import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import com.kornienko.subscriptionsservice.domain.repository.SubscriptionRepository
import com.kornienko.subscriptionsservice.domain.repository.SubscriptionSpecifications
import jakarta.annotation.PostConstruct
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.time.OffsetDateTime
import java.util.UUID
import java.math.BigDecimal
import kotlin.math.exp

@Service
class SubscriptionService (
    val subscriptionRepository: SubscriptionRepository,
    val statusHistoryService: StatusHistoryService,
    val notificationService: NotificationService
){

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun create(request: SubscriptionCreateRequestDto): SubscriptionResponse  {
        log.info("Создание подписки для пользователя: {}", request.userId)

        val start = request.startAt ?: OffsetDateTime.now()

        require(start >= OffsetDateTime.now().minusSeconds(1)) {
            "Дата начала не может быть в прошлом."
        }

        val duration = request.durationMonths ?: 1
        require(duration > 0) { "Длительность должна быть положительной." }

        val end = start.plusMonths(duration)

        val subscription = Subscription(
            userId = request.userId,
            serviceName = request.serviceName,
            status = SubscriptionStatus.ACTIVE,
            description = request.description,
            cost = request.cost,
            startAt = start,
            endAt = end,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        val saved = subscriptionRepository.saveAndFlush(subscription)

        notificationService.createExpiringSoonNotification(saved.id, saved.endAt)

        statusHistoryService.recordStatusChange(
            subscriptionId = saved.id,
            oldStatus = null,
            newStatus = SubscriptionStatus.ACTIVE,
            changedBy = "user"
        )
        return SubscriptionResponse.from(saved)
    }

    /**
     * Получение всех подписок с фильтрацией и пагинацией
     */
    fun findAll(filter: SubscriptionFilter, pageable: Pageable): Page<SubscriptionResponse> {
        val spec = SubscriptionSpecifications.buildSpecification(filter)

        val start = OffsetDateTime.now()

        println(start)
        log.info("Repository findAll with pageable: page={}, size={}, sort={}",
            pageable.pageNumber, pageable.pageSize, pageable.sort)

        val result = subscriptionRepository.findAll(spec, pageable)

        log.info("Repository returned: totalElements={}, totalPages={}, contentSize={}",
            result.totalElements, result.totalPages, result.content.size)

        return result.map { SubscriptionResponse.from(it) }
    }


    fun findById(id: UUID): SubscriptionResponse {
        val subscription = findSubscriptionOrThrow(id)
        return SubscriptionResponse.from(subscription)
    }

    private fun findSubscriptionOrThrow(id: UUID): Subscription {
        return subscriptionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Подписка с ID не найдена: $id") }
    }

    /**
     * Получение активных подписок пользователя
     */
    fun getActiveSubscriptions(userId: UUID): List<SubscriptionResponse> {
        val now = OffsetDateTime.now()
        val subscriptions = subscriptionRepository.findActiveByUserId(userId, now)
        return subscriptions.map { SubscriptionResponse.from(it) }
    }

    /**
     * Частичное обновление подписки
     *
     * @param id ID подписки
     * @param request DTO с полями для обновления (не null поля будут обновлены)
     * @return Обновлённая подписка в виде DTO
     * @throws NoSuchElementException если подписка не найдена
     * @throws IllegalArgumentException если передан невалидный статус или стоимость
     */
    @Transactional
    fun updateSubscription(id: UUID, request: SubscriptionUpdateRequest): SubscriptionResponse {
        val subscription = findSubscriptionOrThrow(id)

        request.serviceName?.let {
            require(it.isNotBlank()) { "Название сервиса не может быть пустым" }
            subscription.serviceName = it
        }

        request.description?.let {
            subscription.description = it
        }


        request.cost?.let {
            validateCost(it)
            subscription.cost = it
        }

        // Обновляем статус, если он передан
        request.status?.let { newStatus ->
            updateStatusInternal(subscription, newStatus)
        }

        request.endAt?.let { newEndAt ->
            require(newEndAt.isAfter(subscription.startAt)) {
                "Дата окончания должна быть позже даты начала (${subscription.startAt})"
            }
            require(newEndAt.isAfter(OffsetDateTime.now())) {
                "Дата окончания не может быть в прошлом"
            }
            subscription.endAt = newEndAt

            // Если подписка была EXPIRED и мы продлили дату, можно её активировать
            if (subscription.status == SubscriptionStatus.EXPIRED && newEndAt.isAfter(OffsetDateTime.now())) {
                subscription.status = SubscriptionStatus.ACTIVE
                statusHistoryService.recordStatusChange(
                    subscriptionId = id,
                    oldStatus = SubscriptionStatus.EXPIRED,
                    newStatus = SubscriptionStatus.ACTIVE,
                    changedBy = "user"
                )
            }
        }
        log.info("Версия: {}",subscription.version)
        val updated = subscriptionRepository.saveAndFlush(subscription)

        log.info("Подписка {} обновлена", id)
        log.info("Версия: {}",updated.version)
        return SubscriptionResponse.from(updated)
    }

    /**
     * Обновление статуса подписки
     *
     * @param id Идентификатор подписки
     * @param newStatus Новый статус
     * @return Обновлённая подписка в виде DTO
     * @throws IllegalArgumentException если переход статуса невозможен
     */
    @Transactional
    fun updateStatus(id: UUID, newStatus: SubscriptionStatus): SubscriptionResponse {
        val subscription = findSubscriptionOrThrow(id)
        updateStatusInternal(subscription, newStatus)

        val updated = subscriptionRepository.saveAndFlush(subscription)
        log.info("Статус подписки {} изменён на {}", id, newStatus)

        return SubscriptionResponse.from(updated)
    }

    /**
     * Отмена подписки
     *
     * @param id Идентификатор подписки
     * @return Отменённая подписка в виде DTO
     */
    @Transactional
    fun cancelSubscription(id: UUID): SubscriptionResponse {
        return updateStatus(id, SubscriptionStatus.CANCELLED)
    }

    /**
     * Приостановка подписки
     *
     * @param id Идентификатор подписки
     * @return Приостановленная подписка в виде DTO
     */
    @Transactional
    fun pauseSubscription(id: UUID): SubscriptionResponse {
        return updateStatus(id, SubscriptionStatus.PAUSED)
    }

    /**
     * Внутренний метод обновления статуса с валидацией
     *
     * Проверяет возможность перехода, дополнительные условия
     * и записывает изменение в историю
     *
     * @param subscription Сущность подписки
     * @param newStatus Новый статус
     * @throws IllegalArgumentException если переход невозможен
     */
    private fun updateStatusInternal(subscription: Subscription, newStatus: SubscriptionStatus) {
        val oldStatus = subscription.status

        // Проверка допустимости перехода (конечный автомат статусов)
        require(oldStatus.canTransitionTo(newStatus)) {
            "Невозможно перейти из статуса $oldStatus в $newStatus"
        }

        when (newStatus) {
            SubscriptionStatus.ACTIVE -> {
                require(subscription.endAt.isAfter(OffsetDateTime.now())) {
                    "Нельзя активировать истекшую подписку"
                }
            }
            SubscriptionStatus.CANCELLED -> {
                log.info("Подписка {} отменена пользователем", subscription.id)
            }
            SubscriptionStatus.PAUSED -> {
                require(subscription.endAt.isAfter(OffsetDateTime.now())) {
                    "Нельзя приостановить истекшую подписку"
                }
            }
            else -> Unit
        }

        subscription.status = newStatus

        statusHistoryService.recordStatusChange(
            subscriptionId = subscription.id,
            oldStatus = oldStatus,
            newStatus = newStatus,
            changedBy = "user"
        )
    }

    @Transactional
    fun expireSubscriptionsBatch(batchSize: Int): Int {
        val now = OffsetDateTime.now()
        // Находим просроченные подписки с блокировкой строк
        val expiredSubscriptions = subscriptionRepository.findExpiredSubscriptionsForUpdate(now, batchSize)

        println(expiredSubscriptions)
        if (expiredSubscriptions.isEmpty()) {
            return 0
        }

        expiredSubscriptions.forEach { subscription ->
            try {
                val oldStatus = subscription.status
                subscription.status = SubscriptionStatus.EXPIRED
                subscription.updatedAt = OffsetDateTime.now()
                subscriptionRepository.save(subscription)

                // Записываем в историю, что подписка истекла автоматически
                statusHistoryService.recordStatusChange(
                    subscriptionId = subscription.id,
                    oldStatus = oldStatus,
                    newStatus = SubscriptionStatus.EXPIRED
                )

                log.debug("Подписка {} автоматически переведена в статус EXPIRED", subscription.id)
            } catch (e: Exception) {
                log.error("Ошибка при переводе подписки {} в статус EXPIRED", subscription.id, e)
            }
        }

        log.info("Переведено в статус EXPIRED {} подписок", expiredSubscriptions.size)
        return expiredSubscriptions.size
    }

    /**
     * Возобновление подписки (из статуса PAUSED)
     *
     * @param id Идентификатор подписки
     * @return Возобновлённая подписка в виде DTO
     * @throws IllegalArgumentException если подписка не приостановлена или истекла
     */
    @Transactional
    fun resumeSubscription(id: UUID): SubscriptionResponse {
        val subscription = findSubscriptionOrThrow(id)

        require(subscription.status == SubscriptionStatus.PAUSED) {
            "Возобновить можно только приостановленную подписку. Текущий статус: ${subscription.status}"
        }

        require(subscription.endAt.isAfter(OffsetDateTime.now())) {
            "Нельзя возобновить истекшую подписку"
        }

        return updateStatus(id, SubscriptionStatus.ACTIVE)
    }

    /**
     * Продление подписки
     *
     * @param id Идентификатор подписки
     * @param months Количество месяцев для продления
     * @return Продлённая подписка
     * @throws IllegalArgumentException если подписка отменена или некорректные параметры
     */
    @Transactional
    fun renewSubscription(id: UUID, months: Long): SubscriptionResponse {
        require(months in 1..120) {
            "Количество месяцев должно быть от 1 до 120, получено: $months"
        }

        val subscription = findSubscriptionOrThrow(id)

        require(subscription.status != SubscriptionStatus.CANCELLED) {
            "Нельзя продлить отменённую подписку"
        }

        val now = OffsetDateTime.now()
        val newEndAt = if (subscription.endAt.isAfter(now)) {
            // Если подписка ещё активна - продлеваем от текущей даты окончания
            subscription.endAt.plusMonths(months)
        } else {
            // Если подписка уже истекла - продлеваем от текущего момента
            now.plusMonths(months)
        }

        subscription.endAt = newEndAt
        subscription.updatedAt = now

        // Если подписка была истекшей или приостановленной - активируем её
        val wasExpiredOrPaused = subscription.status == SubscriptionStatus.EXPIRED ||
                subscription.status == SubscriptionStatus.PAUSED

        if (wasExpiredOrPaused) {
            subscription.status = SubscriptionStatus.ACTIVE
            statusHistoryService.recordStatusChange(
                subscriptionId = id,
                oldStatus = subscription.status,
                newStatus = SubscriptionStatus.ACTIVE,
                changedBy = "user"
            )
        }

        val updated = subscriptionRepository.saveAndFlush(subscription)


        statusHistoryService.recordStatusChange(
            subscriptionId = id,
            oldStatus = null,
            newStatus = SubscriptionStatus.ACTIVE,
            changedBy = "system"
        )

        log.info("Подписка {} продлена на {} месяцев. Новая дата окончания: {}", id, months, newEndAt)

        notificationService.createExpiringSoonNotification(updated.id, updated.endAt)

        return SubscriptionResponse.from(updated)
    }




    private fun validateCost(cost: BigDecimal) {
        require(cost >= BigDecimal.ZERO) {
            "Стоимость не может быть отрицательной: $cost"
        }
        require(cost <= BigDecimal("99999999.99")) {
            "Стоимость не может превышать 99 999 999.99: $cost"
        }
    }

}