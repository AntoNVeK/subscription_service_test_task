package com.kornienko.subscriptionsservice.domain.repository

import com.kornienko.subscriptionsservice.domain.Subscription
import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.OffsetDateTime
import java.util.*

interface SubscriptionRepository : JpaRepository<Subscription, UUID>,
    JpaSpecificationExecutor<Subscription> {


    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId AND s.status = 'ACTIVE' AND s.endAt > :now")
    fun findActiveByUserId(userId: UUID, now: OffsetDateTime): List<Subscription>


    /**
     * Находит просроченные подписки для автоматического перевода в статус EXPIRED
     * Использует пессимистическую блокировку с пропуском уже заблокированных строк
     *
     * @param now текущее время
     * @param batchSize максимальное количество записей за одну операцию
     */
    @Query(
        value = """
            SELECT * FROM subscriptions 
            WHERE status = 'ACTIVE' AND end_at < :now
            ORDER BY end_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true
    )
    fun findExpiredSubscriptionsForUpdate(
        @Param("now") now: OffsetDateTime,
        @Param("batchSize") batchSize: Int
    ): List<Subscription>


    fun findByUserId(userId: UUID, pageable: Pageable): Page<Subscription>
    fun findByStatus(status: SubscriptionStatus, pageable: Pageable): Page<Subscription>
}