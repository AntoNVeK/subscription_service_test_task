package com.kornienko.subscriptionsservice.api

import com.kornienko.subscriptionsservice.domain.service.StatusHistoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "История подписок", description = "API для истории статусов подписки")
class SubscriptionHistoryController(
    private val statusHistoryService: StatusHistoryService
) {

    @GetMapping("/{subscriptionId}/history")
    @Operation(summary = "Получить полную историю статусов подписки")
    fun getHistory(
        @PathVariable subscriptionId: UUID,
        @PageableDefault(size = 20, sort = ["changedAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): Page<StatusHistoryResponse> {
        return statusHistoryService.getHistory(subscriptionId, pageable)
            .map { StatusHistoryResponse.from(it) }
    }

    @GetMapping("/{subscriptionId}/history/latest")
    @Operation(summary = "Получить последнее изменение статуса")
    fun getLatestStatus(@PathVariable subscriptionId: UUID): StatusHistoryResponse? {
        val latest = statusHistoryService.getHistory(subscriptionId).firstOrNull()
        return latest?.let { StatusHistoryResponse.from(it) }
    }

    @GetMapping("/{subscriptionId}/history/count")
    @Operation(summary = "Получить количество изменений статуса")
    fun getStatusChangeCount(@PathVariable subscriptionId: UUID): Map<String, Long> {
        val count = statusHistoryService.getStatusChangeCount(subscriptionId)
        return mapOf("statusChanges" to count)
    }
}