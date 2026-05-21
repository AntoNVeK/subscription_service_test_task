package com.kornienko.subscriptionsservice
import com.kornienko.subscriptionsservice.api.SubscriptionCreateRequestDto
import com.kornienko.subscriptionsservice.api.SubscriptionUpdateRequest
import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import com.kornienko.subscriptionsservice.domain.service.SubscriptionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*
import org.junit.jupiter.api.assertThrows

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SubscriptionOptimisticLockTest {

    @Autowired
    private lateinit var subscriptionService: SubscriptionService

    @Test
    fun shouldThrowOptimisticLockWhenConcurrentUpdate() {
        // 1. Создаём подписку
        val createRequest = SubscriptionCreateRequestDto(
            userId = UUID.randomUUID(),
            serviceName = "Original Service",
            cost = BigDecimal("10.00"),
            startAt = OffsetDateTime.now().plusDays(1),
            durationMonths = 1
        )
        val subscription = subscriptionService.create(createRequest)
        val subscriptionId = subscription.id


        println("ID: $subscriptionId")
        println("Начальная версия: ${subscription.version}")

        // 2. Запускаем два параллельных обновления
        val exceptions = Collections.synchronizedList(mutableListOf<Throwable>())
        val successes = Collections.synchronizedList(mutableListOf<String>())

        val tasks = listOf(
            {
                try {
                    subscriptionService.updateSubscription(
                        subscriptionId,
                        SubscriptionUpdateRequest(serviceName = "Updated by Task 1")
                    )
                    successes.add("Task 1")
                    println("Task 1: Успешно")
                } catch (e: Exception) {
                    exceptions.add(e)
                    println("Task 1: Ошибка - ${e.javaClass.simpleName}: ${e.message}")
                }
            },
            {
                try {
                    subscriptionService.updateSubscription(
                        subscriptionId,
                        SubscriptionUpdateRequest(serviceName = "Updated by Task 2")
                    )
                    successes.add("Task 2")
                    println("Task 2: Успешно")
                } catch (e: Exception) {
                    exceptions.add(e)
                    println("Task 2: Ошибка - ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        )

        tasks.parallelStream().forEach { it.invoke() }


        println("Успешных обновлений: ${successes.size}")
        println("Ошибок: ${exceptions.size}")

        if (exceptions.isNotEmpty()) {
            println("Тип ошибки: ${exceptions[0].javaClass.simpleName}")
        }

        val finalSubscription = subscriptionService.findById(subscriptionId)
        println("Финальная версия: ${finalSubscription.version}")
        println("Финальное название: ${finalSubscription.serviceName}")

        // Должна быть хотя бы одна ошибка OptimisticLockingFailureException
        val hasOptimisticLockException = exceptions.any { it is OptimisticLockingFailureException }
        assertThat(hasOptimisticLockException).isTrue()

        println("Тест пройден: оптимистическая блокировка сработала!")
    }

    @Test
    fun shouldUpdateSuccessfullyWhenNoConflict() {
        // Создаём подписку
        val createRequest = SubscriptionCreateRequestDto(
            userId = UUID.randomUUID(),
            serviceName = "Initial",
            cost = BigDecimal("20.00"),
            startAt = OffsetDateTime.now().plusDays(1),
            durationMonths = 1
        )
        val subscription = subscriptionService.create(createRequest)
        val subscriptionId = subscription.id

        val initialVersion = subscription.version
        println("Начальная версия: $initialVersion")

        // Обновляем подписку
        val updateRequest = SubscriptionUpdateRequest(
            serviceName = "Updated",
            cost = BigDecimal("25.00"),
            description = "New description"
        )
        val updated = subscriptionService.updateSubscription(subscriptionId, updateRequest)

        println("Новая версия: ${updated.version}")

        assertThat(updated.serviceName).isEqualTo("Updated")
        assertThat(updated.cost).isEqualByComparingTo("25.00")
        assertThat(updated.version).isEqualTo(initialVersion + 1)

        println("Тест пройден: обновление без конфликта успешно")
    }

    @Test
    fun shouldUpdateStatusWithOptimisticLock() {
        // Создаём подписку
        val createRequest = SubscriptionCreateRequestDto(
            userId = UUID.randomUUID(),
            serviceName = "Status Test",
            cost = BigDecimal("15.00"),
            startAt = OffsetDateTime.now().plusDays(1),
            durationMonths = 1
        )
        val subscription = subscriptionService.create(createRequest)
        val subscriptionId = subscription.id

        val initialVersion = subscription.version
        println("Начальная версия: $initialVersion, статус: ${subscription.status}")

        // Меняем статус на PAUSED
        val paused = subscriptionService.pauseSubscription(subscriptionId)
        println("После PAUSED: версия ${paused.version}, статус: ${paused.status}")
        assertThat(paused.version).isEqualTo(initialVersion + 1)
        assertThat(paused.status).isEqualTo(SubscriptionStatus.PAUSED)

        // Меняем статус на ACTIVE
        val resumed = subscriptionService.resumeSubscription(subscriptionId)
        println("После ACTIVE: версия ${resumed.version}, статус: ${resumed.status}")
        assertThat(resumed.version).isEqualTo(initialVersion + 2)
        assertThat(resumed.status).isEqualTo(SubscriptionStatus.ACTIVE)

        println("Тест пройден: статусы обновляются корректно")
    }
}