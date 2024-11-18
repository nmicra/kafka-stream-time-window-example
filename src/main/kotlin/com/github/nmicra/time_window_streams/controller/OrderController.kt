package com.github.nmicra.time_window_streams.controller


import com.github.nmicra.time_window_streams.domain.Order
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/orders")
class OrderController(private val kafkaTemplate: KafkaTemplate<String, Order>) {

    @PostMapping
    fun publishOrder(@RequestBody order: Order): String {
        kafkaTemplate.send("ordering", order.id, order)
        return "Order published successfully"
    }
}