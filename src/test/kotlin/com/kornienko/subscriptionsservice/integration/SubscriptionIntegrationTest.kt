package com.kornienko.subscriptionsservice

import com.kornienko.subscriptionsservice.api.SubscriptionCreateRequestDto
import com.kornienko.subscriptionsservice.api.SubscriptionUpdateRequest
import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import com.kornienko.subscriptionsservice.domain.service.SubscriptionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
class SubscriptionIntegrationTest {

    @Autowired
    private lateinit var subscriptionService: SubscriptionService

    @Test
    fun fullSubscriptionLifecycle() {
        // 1. Создание
        val createRequest = SubscriptionCreateRequestDto(
            userId = UUID.randomUUID(),
            serviceName = "Test Service",
            cost = BigDecimal("10.00"),
            description = "Test",
            startAt = OffsetDateTime.now().plusDays(1),
            durationMonths = 1
        )
        val created = subscriptionService.create(createRequest)
        assertThat(created.id).isNotNull

        // 2. Поиск по ID
        val found = subscriptionService.findById(created.id)
        assertThat(found.serviceName).isEqualTo("Test Service")

        // 3. Обновление статуса
        val paused = subscriptionService.updateStatus(created.id, SubscriptionStatus.PAUSED)
        assertThat(paused.status).isEqualTo(SubscriptionStatus.PAUSED)

        // 4. Возобновление
        val resumed = subscriptionService.resumeSubscription(created.id)
        assertThat(resumed.status).isEqualTo(SubscriptionStatus.ACTIVE)

        // 5. Отмена
        val cancelled = subscriptionService.cancelSubscription(created.id)
        assertThat(cancelled.status).isEqualTo(SubscriptionStatus.CANCELLED)
    }

    @Test
    fun shouldUpdateSubscriptionFields() {
        val createRequest = SubscriptionCreateRequestDto(
            userId = UUID.randomUUID(),
            serviceName = "Original",
            cost = BigDecimal("10.00"),
            description = "Original desc",
            startAt = OffsetDateTime.now().plusDays(1),
            durationMonths = 1
        )
        val created = subscriptionService.create(createRequest)

        val updateRequest = SubscriptionUpdateRequest(
            serviceName = "Updated",
            description = "Updated desc",
            cost = BigDecimal("20.00")
        )
        val updated = subscriptionService.updateSubscription(created.id, updateRequest)

        assertThat(updated.serviceName).isEqualTo("Updated")
        assertThat(updated.description).isEqualTo("Updated desc")
        assertThat(updated.cost).isEqualByComparingTo("20.00")
    }
}