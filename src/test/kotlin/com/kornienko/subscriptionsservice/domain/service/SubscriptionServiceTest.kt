package com.kornienko.subscriptionsservice.domain.service

import com.kornienko.subscriptionsservice.api.SubscriptionCreateRequestDto
import com.kornienko.subscriptionsservice.api.SubscriptionUpdateRequest
import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SubscriptionServiceTest {

    @Autowired
    private lateinit var subscriptionService: SubscriptionService

    @Test
    fun shouldCreateSubscriptionSuccessfully() {
        val request = SubscriptionCreateRequestDto(
            userId = UUID.randomUUID(),
            serviceName = "Test Service",
            cost = BigDecimal("10.00"),
            description = "Test Description",
            startAt = OffsetDateTime.now().plusDays(1),
            durationMonths = 1
        )

        val result = subscriptionService.create(request)

        assertThat(result.id).isNotNull
        assertThat(result.serviceName).isEqualTo("Test Service")
        assertThat(result.cost).isEqualByComparingTo("10.00")
        assertThat(result.status).isEqualTo(SubscriptionStatus.ACTIVE)
    }

    @Test
    fun shouldFindSubscriptionById() {
        // Создаём подписку
        val createRequest = SubscriptionCreateRequestDto(
            userId = UUID.randomUUID(),
            serviceName = "Netflix",
            cost = BigDecimal("15.99"),
            startAt = OffsetDateTime.now().plusDays(1),
            durationMonths = 1
        )
        val created = subscriptionService.create(createRequest)

        // Ищем по ID
        val found = subscriptionService.findById(created.id)

        assertThat(found.id).isEqualTo(created.id)
        assertThat(found.serviceName).isEqualTo("Netflix")
    }

    @Test
    fun shouldUpdateStatusFromActiveToPaused() {
        // Создаём подписку
        val createRequest = SubscriptionCreateRequestDto(
            userId = UUID.randomUUID(),
            serviceName = "Spotify",
            cost = BigDecimal("9.99"),
            startAt = OffsetDateTime.now().plusDays(1),
            durationMonths = 1
        )
        val created = subscriptionService.create(createRequest)

        // Меняем статус
        val updated = subscriptionService.updateStatus(created.id, SubscriptionStatus.PAUSED)

        assertThat(updated.status).isEqualTo(SubscriptionStatus.PAUSED)
    }

    @Test
    fun shouldCancelSubscription() {
        val createRequest = SubscriptionCreateRequestDto(
            userId = UUID.randomUUID(),
            serviceName = "HBO",
            cost = BigDecimal("12.99"),
            startAt = OffsetDateTime.now().plusDays(1),
            durationMonths = 1
        )
        val created = subscriptionService.create(createRequest)

        val cancelled = subscriptionService.cancelSubscription(created.id)

        assertThat(cancelled.status).isEqualTo(SubscriptionStatus.CANCELLED)
    }

    @Test
    fun shouldUpdateOnlyProvidedFields() {
        val createRequest = SubscriptionCreateRequestDto(
            userId = UUID.randomUUID(),
            serviceName = "Original Name",
            cost = BigDecimal("10.00"),
            description = "Original description",
            startAt = OffsetDateTime.now().plusDays(1),
            durationMonths = 1
        )
        val created = subscriptionService.create(createRequest)

        // Обновляем только имя
        val updateRequest = SubscriptionUpdateRequest(
            serviceName = "New Name",
            description = null,
            cost = null,
            status = null
        )
        val updated = subscriptionService.updateSubscription(created.id, updateRequest)

        assertThat(updated.serviceName).isEqualTo("New Name")
        assertThat(updated.description).isEqualTo("Original description") // не изменилось
        assertThat(updated.cost).isEqualByComparingTo("10.00") // не изменилось
    }

    @Test
    fun shouldPauseAndResumeSubscription() {
        val createRequest = SubscriptionCreateRequestDto(
            userId = UUID.randomUUID(),
            serviceName = "Music Service",
            cost = BigDecimal("7.99"),
            startAt = OffsetDateTime.now().plusDays(1),
            durationMonths = 1
        )
        val created = subscriptionService.create(createRequest)

        // Приостанавливаем
        val paused = subscriptionService.pauseSubscription(created.id)
        assertThat(paused.status).isEqualTo(SubscriptionStatus.PAUSED)

        // Возобновляем
        val resumed = subscriptionService.resumeSubscription(created.id)
        assertThat(resumed.status).isEqualTo(SubscriptionStatus.ACTIVE)
    }
}