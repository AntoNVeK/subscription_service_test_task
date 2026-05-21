package com.kornienko.subscriptionsservice.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "subscriptions")
class Subscription(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "service_name", nullable = false)
    var serviceName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SubscriptionStatus,

    var description: String?,

    @Column(nullable = false)
    var cost: BigDecimal,

    @Column(name = "start_at", nullable = false)
    var startAt: OffsetDateTime,

    @Column(name = "end_at", nullable = false)
    var endAt: OffsetDateTime,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime

) {
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0


    @PreUpdate
    fun onUpdate() {
        this.updatedAt = OffsetDateTime.now()
    }
}
