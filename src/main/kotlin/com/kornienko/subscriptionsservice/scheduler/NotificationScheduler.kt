package com.kornienko.subscriptionsservice.scheduler

import com.kornienko.subscriptionsservice.domain.service.NotificationService
import com.kornienko.subscriptionsservice.domain.service.SubscriptionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
class NotificationScheduler(
    private val notificationService: NotificationService,
    @Value("\${batch.size:100}")
    private val batchSize: Int
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${notification.scheduler.interval-ms:60000}")
    fun sendNotifications() {
        val sent = notificationService.sendPendingNotifications(batchSize)
        if (sent > 0) {
            log.info("Отправлено уведомлений: {}", sent)
        }
    }
}