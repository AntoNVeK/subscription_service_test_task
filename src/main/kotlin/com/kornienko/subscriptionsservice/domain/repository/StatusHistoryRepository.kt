package com.kornienko.subscriptionsservice.domain.repository

import com.kornienko.subscriptionsservice.domain.StatusHistory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface StatusHistoryRepository : JpaRepository<StatusHistory, UUID> {

    /**
     * Получить всю историю статусов для подписки (от новых к старым)
     */
    fun findBySubscriptionIdOrderByChangedAtDesc(subscriptionId: UUID): List<StatusHistory>

    /**
     * Получить последний статус подписки
     */
    @Query("SELECT h FROM StatusHistory h WHERE h.subscriptionId = :subscriptionId ORDER BY h.changedAt DESC LIMIT 1")
    fun findLatestBySubscriptionId(@Param("subscriptionId") subscriptionId: UUID): StatusHistory?

    /**
     * Получить историю с пагинацией для подписки
     */
    fun findBySubscriptionId(subscriptionId: UUID, pageable: Pageable): Page<StatusHistory>

    /**
     * Подсчитать сколько раз подписка меняла статус
     */
    fun countBySubscriptionId(subscriptionId: UUID): Long

    /**
     * Получить все изменения для списка подписок
     */
    fun findBySubscriptionIdIn(subscriptionIds: List<UUID>): List<StatusHistory>

    /**
     * Удалить историю для подписки (при удалении самой подписки)
     */
    fun deleteBySubscriptionId(subscriptionId: UUID)

}