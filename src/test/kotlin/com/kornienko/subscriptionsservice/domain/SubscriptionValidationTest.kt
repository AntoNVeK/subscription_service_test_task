package com.kornienko.subscriptionsservice.domain

import com.kornienko.subscriptionsservice.api.SubscriptionCreateRequestDto
import com.kornienko.subscriptionsservice.domain.service.SubscriptionService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
class SubscriptionValidationTest {

    @Autowired
    private lateinit var subscriptionService: SubscriptionService

    @Test
    fun shouldThrowExceptionWhenStartDateInPast() {
        val request = SubscriptionCreateRequestDto(
            userId = UUID.randomUUID(),
            serviceName = "Test",
            cost = BigDecimal("10.00"),
            startAt = OffsetDateTime.now().minusDays(1),
            durationMonths = 1
        )

        assertThrows<IllegalArgumentException> {
            subscriptionService.create(request)
        }
    }

    @Test
    fun shouldThrowExceptionWhenDurationIsZero() {
        val request = SubscriptionCreateRequestDto(
            userId = UUID.randomUUID(),
            serviceName = "Test",
            cost = BigDecimal("10.00"),
            startAt = OffsetDateTime.now().plusDays(1),
            durationMonths = 0
        )

        assertThrows<IllegalArgumentException> {
            subscriptionService.create(request)
        }
    }

}