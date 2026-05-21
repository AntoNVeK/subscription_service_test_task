package com.kornienko.subscriptionsservice.domain.repository

import com.kornienko.subscriptionsservice.api.SubscriptionFilter

import com.kornienko.subscriptionsservice.domain.Subscription
import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import org.springframework.data.jpa.domain.Specification
import java.time.OffsetDateTime
import java.util.*

object SubscriptionSpecifications {

    /**
     * Фильтр по userId
     */
    fun byUserId(userId: UUID?): Specification<Subscription>? {
        return userId?.let {
            Specification { root, _, cb ->
                cb.equal(root.get<UUID>("userId"), it)
            }
        }
    }

    /**
     * Фильтр по названию сервиса (частичное совпадение, регистронезависимое)
     */
    fun byServiceName(serviceName: String?): Specification<Subscription>? {
        return serviceName?.takeIf { it.isNotBlank() }?.let {
            Specification { root, _, cb ->
                cb.like(
                    cb.lower(root.get("serviceName")),
                    "%${it.lowercase()}%"
                )
            }
        }
    }

    /**
     * Фильтр по статусу
     */
    fun byStatus(status: SubscriptionStatus?): Specification<Subscription>? {
        return status?.let {
            Specification<Subscription> { root, _, cb ->
                cb.equal(root.get<SubscriptionStatus>("status"), it)
            }
        }
    }

    /**
     * Фильтр по диапазону дат начала
     */
    fun byStartDateRange(from: OffsetDateTime?, to: OffsetDateTime?): Specification<Subscription>? {
        return when {
            from != null && to != null -> {
                Specification { root, _, cb ->
                    cb.between(root.get<OffsetDateTime>("startAt"), from, to)
                }
            }
            from != null -> {
                Specification { root, _, cb ->
                    cb.greaterThanOrEqualTo(root.get<OffsetDateTime>("startAt"), from)
                }
            }
            to != null -> {
                Specification { root, _, cb ->
                    cb.lessThanOrEqualTo(root.get<OffsetDateTime>("startAt"), to)
                }
            }
            else -> null
        }
    }

    /**
     * Фильтр по диапазону дат окончания
     */
    fun byEndDateRange(from: OffsetDateTime?, to: OffsetDateTime?): Specification<Subscription>? {
        return when {
            from != null && to != null -> {
                Specification { root, _, cb ->
                    cb.between(root.get<OffsetDateTime>("endAt"), from, to)
                }
            }
            from != null -> {
                Specification { root, _, cb ->
                    cb.greaterThanOrEqualTo(root.get<OffsetDateTime>("endAt"), from)
                }
            }
            to != null -> {
                Specification { root, _, cb ->
                    cb.lessThanOrEqualTo(root.get<OffsetDateTime>("endAt"), to)
                }
            }
            else -> null
        }
    }

    /**
     * Комбинирует все фильтры в одну Specification
     */
    fun buildSpecification(filter: SubscriptionFilter): Specification<Subscription> {
        var spec = Specification.where<Subscription> { _, _, _ -> null }

        byUserId(filter.userId)?.let { spec = spec.and(it) }
        byServiceName(filter.serviceName)?.let { spec = spec.and(it) }
        byStatus(filter.status)?.let { spec = spec.and(it) }
        byStartDateRange(filter.startDateFrom, filter.startDateTo)?.let { spec = spec.and(it) }
        byEndDateRange(filter.endDateFrom, filter.endDateTo)?.let { spec = spec.and(it) }

        return spec
    }
}