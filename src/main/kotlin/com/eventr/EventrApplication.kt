package com.eventr

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class EventrApplication

fun main(args: Array<String>) {
    runApplication<EventrApplication>(*args)
}
