package com.kornienko.subscriptionsservice.domain.repository

import com.kornienko.subscriptionsservice.domain.Subscription
import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SubscriptionRepositoryTest {

    @Autowired
    private lateinit var repository: SubscriptionRepository

    private lateinit var testSubscription: Subscription

    @BeforeEach
    fun setUp() {
        val now = OffsetDateTime.now()
        testSubscription = Subscription(
            userId = UUID.randomUUID(),
            serviceName = "Netflix",
            status = SubscriptionStatus.ACTIVE,
            description = "Premium plan",
            cost = BigDecimal("15.99"),
            startAt = now,
            endAt = now.plusMonths(1),
            createdAt = now,
            updatedAt = now
        )
    }

    @Test
    fun shouldSaveSubscription() {
        val saved = repository.save(testSubscription)
        assertThat(saved.id).isNotNull
        assertThat(saved.serviceName).isEqualTo("Netflix")
    }

    @Test
    fun shouldFindActiveUserSubscriptions() {
        val userId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        val active = Subscription(
            userId = userId,
            serviceName = "Spotify",
            status = SubscriptionStatus.ACTIVE,
            description = null,
            cost = BigDecimal("9.99"),
            startAt = now,
            endAt = now.plusMonths(1),
            createdAt = now,
            updatedAt = now
        )

        repository.save(active)

        val result = repository.findActiveByUserId(userId, now)

        assertThat(result).hasSize(1)
        assertThat(result[0].serviceName).isEqualTo("Spotify")
    }

    @Test
    fun shouldFindExpiredSubscriptions() {
        val now = OffsetDateTime.now()

        val expired = Subscription(
            userId = UUID.randomUUID(),
            serviceName = "Expired",
            status = SubscriptionStatus.ACTIVE,
            description = null,
            cost = BigDecimal("5.99"),
            startAt = now.minusMonths(2),
            endAt = now.minusDays(1),
            createdAt = now.minusMonths(2),
            updatedAt = now.minusMonths(2)
        )

        repository.save(expired)

        val result = repository.findExpiredSubscriptionsForUpdate(now, 10)

        assertThat(result).hasSize(1)
        assertThat(result[0].serviceName).isEqualTo("Expired")
    }

    @Test
    fun shouldFindByUserIdWithPagination() {
        val userId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        (1..25).forEach { i ->
            repository.save(
                Subscription(
                    userId = userId,
                    serviceName = "Service $i",
                    status = SubscriptionStatus.ACTIVE,
                    description = null,
                    cost = BigDecimal.TEN,
                    startAt = now,
                    endAt = now.plusMonths(1),
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        val firstPage = repository.findByUserId(userId, PageRequest.of(0, 10))
        val secondPage = repository.findByUserId(userId, PageRequest.of(1, 10))

        assertThat(firstPage.totalElements).isEqualTo(25)
        assertThat(firstPage.content).hasSize(10)
        assertThat(secondPage.content).hasSize(10)
    }

    @Test
    fun shouldFindByStatusWithPagination() {
        val now = OffsetDateTime.now()

        (1..15).forEach { i ->
            repository.save(
                Subscription(
                    userId = UUID.randomUUID(),
                    serviceName = "Active $i",
                    status = SubscriptionStatus.ACTIVE,
                    description = null,
                    cost = BigDecimal.TEN,
                    startAt = now,
                    endAt = now.plusMonths(1),
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        val result = repository.findByStatus(SubscriptionStatus.ACTIVE, PageRequest.of(0, 10))

        assertThat(result.totalElements).isGreaterThanOrEqualTo(15)
        assertThat(result.content).hasSize(10)
    }

    @Test
    fun shouldApplySorting() {
        val userId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        repository.save(
            Subscription(
                userId = userId,
                serviceName = "A",
                status = SubscriptionStatus.ACTIVE,
                description = null,
                cost = BigDecimal("100"),
                startAt = now,
                endAt = now.plusMonths(1),
                createdAt = now,
                updatedAt = now
            )
        )
        repository.save(
            Subscription(
                userId = userId,
                serviceName = "B",
                status = SubscriptionStatus.ACTIVE,
                description = null,
                cost = BigDecimal("50"),
                startAt = now,
                endAt = now.plusMonths(1),
                createdAt = now,
                updatedAt = now
            )
        )

        val pageable = PageRequest.of(0, 10, Sort.by("cost").descending())
        val result = repository.findByUserId(userId, pageable)

        assertThat(result.content[0].cost).isEqualByComparingTo("100")
        assertThat(result.content[1].cost).isEqualByComparingTo("50")
    }
}