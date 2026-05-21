package com.kornienko.subscriptionsservice.scheduler

import com.kornienko.subscriptionsservice.domain.service.SubscriptionService
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SubscriptionScheduler(
    private val subscriptionService: SubscriptionService,
    @Value("\${batch.size:100}")
    private val batchSize: Int
) {

    @Scheduled(fixedDelayString = "\${task-execution.poller.poll-interval-ms:60000}")
    fun expireSubscriptions() {
        println("Выполняю задачу...")
        println(batchSize)
        subscriptionService.expireSubscriptionsBatch(batchSize)
    }
}