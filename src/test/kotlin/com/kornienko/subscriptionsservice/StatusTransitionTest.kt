package com.kornienko.subscriptionsservice

import com.kornienko.subscriptionsservice.domain.SubscriptionStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StatusTransitionTest {

    @Test
    fun shouldAllowValidTransitions() {
        // ACTIVE -> PAUSED
        assertThat(SubscriptionStatus.ACTIVE.canTransitionTo(SubscriptionStatus.PAUSED)).isTrue

        // ACTIVE -> CANCELLED
        assertThat(SubscriptionStatus.ACTIVE.canTransitionTo(SubscriptionStatus.CANCELLED)).isTrue

        // ACTIVE -> EXPIRED
        assertThat(SubscriptionStatus.ACTIVE.canTransitionTo(SubscriptionStatus.EXPIRED)).isTrue

        // PAUSED -> ACTIVE
        assertThat(SubscriptionStatus.PAUSED.canTransitionTo(SubscriptionStatus.ACTIVE)).isTrue

        // PAUSED -> CANCELLED
        assertThat(SubscriptionStatus.PAUSED.canTransitionTo(SubscriptionStatus.CANCELLED)).isTrue
    }

    @Test
    fun shouldNotAllowInvalidTransitions() {
        // ACTIVE -> ACTIVE
        assertThat(SubscriptionStatus.ACTIVE.canTransitionTo(SubscriptionStatus.ACTIVE)).isFalse

        // CANCELLED -> anything
        assertThat(SubscriptionStatus.CANCELLED.canTransitionTo(SubscriptionStatus.ACTIVE)).isFalse
        assertThat(SubscriptionStatus.CANCELLED.canTransitionTo(SubscriptionStatus.PAUSED)).isFalse

        // EXPIRED -> anything
        assertThat(SubscriptionStatus.EXPIRED.canTransitionTo(SubscriptionStatus.ACTIVE)).isFalse
        assertThat(SubscriptionStatus.EXPIRED.canTransitionTo(SubscriptionStatus.PAUSED)).isFalse
    }
}