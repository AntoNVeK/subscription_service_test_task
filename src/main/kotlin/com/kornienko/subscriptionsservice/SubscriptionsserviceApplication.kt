package com.kornienko.subscriptionsservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class SubscriptionsserviceApplication

fun main(args: Array<String>) {
	runApplication<SubscriptionsserviceApplication>(*args)
}
