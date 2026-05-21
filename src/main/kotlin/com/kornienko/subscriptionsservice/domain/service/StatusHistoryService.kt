package com.kornienko.subscriptionsservice.domain.service

import com.kornienko.subscriptionsservice.domain.StatusHistory
import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import com.kornienko.subscriptionsservice.domain.repository.StatusHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

@Service
@Transactional
class StatusHistoryService(
    private val historyRepository: StatusHistoryRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Записать изменение статуса
     */
    fun recordStatusChange(
        subscriptionId: UUID,
        oldStatus: SubscriptionStatus?,
        newStatus: SubscriptionStatus,
        changedBy: String = "system"
    ) {
        val history = StatusHistory(
            subscriptionId = subscriptionId,
            oldStatus = oldStatus,
            newStatus = newStatus,
            changedAt = OffsetDateTime.now(),
            changedBy = changedBy
        )

        historyRepository.save(history)
        log.debug("Зафиксировано изменение статуса: подписка {}: {} -> {} (by: {})",
            subscriptionId, oldStatus, newStatus, changedBy)
    }

    /**
     * Получить всю историю подписки
     */
    @Transactional(readOnly = true)
    fun getHistory(subscriptionId: UUID): List<StatusHistory> {
        return historyRepository.findBySubscriptionIdOrderByChangedAtDesc(subscriptionId)
    }

    /**
     * Получить историю с пагинацией
     */
    @Transactional(readOnly = true)
    fun getHistory(subscriptionId: UUID, pageable: Pageable): Page<StatusHistory> {
        return historyRepository.findBySubscriptionId(subscriptionId, pageable)
    }

    /**
     * Получить последний статус подписки
     */
    @Transactional(readOnly = true)
    fun getLastStatus(subscriptionId: UUID): SubscriptionStatus? {
        return historyRepository.findLatestBySubscriptionId(subscriptionId)?.newStatus
    }

    /**
     * Получить количество изменений статуса
     */
    @Transactional(readOnly = true)
    fun getStatusChangeCount(subscriptionId: UUID): Long {
        return historyRepository.countBySubscriptionId(subscriptionId)
    }

    /**
     * Получить историю для нескольких подписок
     */
    @Transactional(readOnly = true)
    fun getHistoryForSubscriptions(subscriptionIds: List<UUID>): List<StatusHistory> {
        return historyRepository.findBySubscriptionIdIn(subscriptionIds)
    }

    /**
     * Очистить историю подписки (при удалении)
     */
    fun deleteHistoryForSubscription(subscriptionId: UUID) {
        historyRepository.deleteBySubscriptionId(subscriptionId)
        log.info("История для подписки удалена: {}", subscriptionId)
    }
}