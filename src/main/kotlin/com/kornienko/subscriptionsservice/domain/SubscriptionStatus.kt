package com.kornienko.subscriptionsservice.domain

enum class SubscriptionStatus {
    ACTIVE,
    PAUSED,
    CANCELLED,
    EXPIRED;

    fun canTransitionTo(newStatus: SubscriptionStatus): Boolean {
        return when (this) {
            ACTIVE -> newStatus in setOf(PAUSED, CANCELLED, EXPIRED)
            PAUSED -> newStatus in setOf(ACTIVE, CANCELLED)
            CANCELLED -> false
            EXPIRED -> false
        }
    }
}