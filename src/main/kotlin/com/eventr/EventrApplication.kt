package com.eventr

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EventrApplication

fun main(args: Array<String>) {
    runApplication<EventrApplication>(*args)
}
