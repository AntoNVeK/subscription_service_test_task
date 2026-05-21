package com.kornienko.subscriptionsservice.domain.service

import com.kornienko.subscriptionsservice.domain.Notification
import com.kornienko.subscriptionsservice.domain.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun createExpiringSoonNotification(subscriptionId: UUID, endAt: OffsetDateTime) {
        val message = "Ваша подписка истекает ${endAt.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
        val notification = Notification(
            subscriptionId = subscriptionId,
            message = message,
            scheduledAt = endAt.minusDays(3)
        )
        notificationRepository.save(notification)
        log.info("Создано уведомление для подписки {}", subscriptionId)
    }

    @Transactional
    fun sendPendingNotifications(batchSize: Int = 100): Int {
        val now = OffsetDateTime.now()
        val pending = notificationRepository.findPendingNotificationsForUpdate(now, batchSize)

        if (pending.isEmpty()) return 0

        var sentCount = 0
        pending.forEach { notification ->
            try {
                log.info("📧 Уведомление для подписки {}: {}", notification.subscriptionId, notification.message)
                notificationRepository.markAsSent(notification.id, now)
                sentCount++
            } catch (e: Exception) {
                log.error("Ошибка отправки уведомления {}", notification.id, e)
            }
        }

        return sentCount
    }

    fun getBySubscriptionId(subscriptionId: UUID): List<Notification> {
        return notificationRepository.findBySubscriptionId(subscriptionId)
    }
}