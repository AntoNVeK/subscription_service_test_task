package com.kornienko.subscriptionsservice.domain.service

import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class StatusHistoryServiceTest {

    @Autowired
    private lateinit var statusHistoryService: StatusHistoryService

    @Autowired
    private lateinit var subscriptionService: SubscriptionService

    @Test
    fun shouldRecordStatusChange() {
        // given
        val subscriptionId = UUID.randomUUID()

        // when
        statusHistoryService.recordStatusChange(
            subscriptionId = subscriptionId,
            oldStatus = null,
            newStatus = SubscriptionStatus.ACTIVE
        )

        // then
        val history = statusHistoryService.getHistory(subscriptionId)
        assertThat(history).hasSize(1)
        assertThat(history[0].newStatus).isEqualTo(SubscriptionStatus.ACTIVE)
        assertThat(history[0].oldStatus).isNull()
    }

    @Test
    fun shouldGetHistoryWithPagination() {
        // given
        val subscriptionId = UUID.randomUUID()

        // Записываем несколько изменений
        statusHistoryService.recordStatusChange(subscriptionId, null, SubscriptionStatus.ACTIVE)
        statusHistoryService.recordStatusChange(subscriptionId, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED)
        statusHistoryService.recordStatusChange(subscriptionId, SubscriptionStatus.PAUSED, SubscriptionStatus.ACTIVE)
        statusHistoryService.recordStatusChange(subscriptionId, SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED)

        // when
        val pageable = PageRequest.of(0, 2, Sort.by("changedAt").descending())
        val page = statusHistoryService.getHistory(subscriptionId, pageable)

        // then
        assertThat(page.totalElements).isEqualTo(4)
        assertThat(page.content).hasSize(2)
        assertThat(page.number).isEqualTo(0)
        assertThat(page.totalPages).isEqualTo(2)
    }

    @Test
    fun shouldGetLastStatus() {
        // given
        val subscriptionId = UUID.randomUUID()

        statusHistoryService.recordStatusChange(subscriptionId, null, SubscriptionStatus.ACTIVE)
        statusHistoryService.recordStatusChange(subscriptionId, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED)
        statusHistoryService.recordStatusChange(subscriptionId, SubscriptionStatus.PAUSED, SubscriptionStatus.CANCELLED)

        // when
        val lastStatus = statusHistoryService.getLastStatus(subscriptionId)

        // then
        assertThat(lastStatus).isEqualTo(SubscriptionStatus.CANCELLED)
    }

    @Test
    fun shouldGetStatusChangeCount() {
        // given
        val subscriptionId = UUID.randomUUID()

        statusHistoryService.recordStatusChange(subscriptionId, null, SubscriptionStatus.ACTIVE)
        statusHistoryService.recordStatusChange(subscriptionId, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED)
        statusHistoryService.recordStatusChange(subscriptionId, SubscriptionStatus.PAUSED, SubscriptionStatus.ACTIVE)

        // when
        val count = statusHistoryService.getStatusChangeCount(subscriptionId)

        // then
        assertThat(count).isEqualTo(3)
    }

    @Test
    fun shouldGetHistoryForMultipleSubscriptions() {
        // given
        val subscriptionId1 = UUID.randomUUID()
        val subscriptionId2 = UUID.randomUUID()

        statusHistoryService.recordStatusChange(subscriptionId1, null, SubscriptionStatus.ACTIVE)
        statusHistoryService.recordStatusChange(subscriptionId1, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED)
        statusHistoryService.recordStatusChange(subscriptionId2, null, SubscriptionStatus.ACTIVE)

        // when
        val history = statusHistoryService.getHistoryForSubscriptions(listOf(subscriptionId1, subscriptionId2))

        // then
        assertThat(history).hasSize(3)
    }

    @Test
    fun shouldDeleteHistoryForSubscription() {
        // given
        val subscriptionId = UUID.randomUUID()
        statusHistoryService.recordStatusChange(subscriptionId, null, SubscriptionStatus.ACTIVE)

        assertThat(statusHistoryService.getStatusChangeCount(subscriptionId)).isEqualTo(1)

        // when
        statusHistoryService.deleteHistoryForSubscription(subscriptionId)

        // then
        assertThat(statusHistoryService.getStatusChangeCount(subscriptionId)).isEqualTo(0)
    }
}