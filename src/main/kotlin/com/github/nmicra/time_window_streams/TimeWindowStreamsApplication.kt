package com.github.nmicra.time_window_streams

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TimeWindowStreamsApplication

fun main(args: Array<String>) {
	runApplication<TimeWindowStreamsApplication>(*args)
}
