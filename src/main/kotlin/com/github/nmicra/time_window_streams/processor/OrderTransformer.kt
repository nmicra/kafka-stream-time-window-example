package com.github.nmicra.time_window_streams.processor

import com.github.nmicra.time_window_streams.domain.Order
import com.github.nmicra.time_window_streams.domain.OrderState
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.processor.Cancellable
import org.apache.kafka.streams.processor.Punctuator
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit

class OrderTransformer(private val stateStoreName: String) : Transformer<String, Order, KeyValue<String, Order>> {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private lateinit var stateStore: KeyValueStore<String, OrderState>
    private lateinit var context: ProcessorContext
    private val punctuators = mutableMapOf<String, Cancellable>()

    override fun init(context: ProcessorContext) {
        this.context = context
        stateStore = context.getStateStore(stateStoreName)
        logger.debug(">>> Initializing OrderTransformer")

        // Restore per-key punctuators after a restart
        val iterator = stateStore.all()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val key = entry.key
            val orderState = entry.value
            val timeLeft = (orderState.windowStartTime + 60_000) - context.currentSystemTimeMs()
            logger.debug(">>> Restoring key: $key, windowStartTime: ${orderState.windowStartTime}, current time: ${context.currentSystemTimeMs()}, timeLeft: $timeLeft")
            if (timeLeft > 0) {
                schedulePunctuator(key, timeLeft)
            } else {
                // If the time has already passed, emit the record and clean up
                logger.debug(">>> Time has already passed for key: $key, emitting record")
                context.forward(key, orderState.highestVersionOrder)
                stateStore.delete(key)
            }
        }
        iterator.close()
    }

    override fun transform(key: String, value: Order): KeyValue<String, Order>? {
        val currentTime = context.currentSystemTimeMs()
        logger.debug("Transform called with key: $key, value: $value, current time: $currentTime")

        var orderState = stateStore.get(key)

        if (orderState == null) {
            // First event for this key
            orderState = OrderState(value, currentTime)
            stateStore.put(key, orderState)
            // Schedule a punctuator for 1 minute later
            schedulePunctuator(key, 60_000)
        } else {
            // Update the highest version order if necessary
            if (value.version > orderState.highestVersionOrder.version) {
                orderState.highestVersionOrder = value
                stateStore.put(key, orderState)
            }
            // No need to reschedule the punctuator; it was set when the first event arrived
        }

        // We don't emit anything now; we emit after 1 minute
        return null
    }

    private fun schedulePunctuator(key: String, delay: Long) {
        logger.debug(">>>  Scheduling punctuator for key: $key with delay: $delay ms")
        val cancellable = context.schedule(
            Duration.of(delay, ChronoUnit.MILLIS),
            org.apache.kafka.streams.processor.PunctuationType.WALL_CLOCK_TIME
        ) {
            logger.debug(">>> Punctuator triggered for key: $key")
            val orderState = stateStore.get(key)
            if (orderState != null) {
                logger.debug(">>> Emitting highest version order: ${orderState.highestVersionOrder} for key: $key")
                context.forward(key, orderState.highestVersionOrder)
                stateStore.delete(key)
                punctuators.remove(key)
            } else {
                logger.debug(">>> No state found for key: $key")
            }
        }
        punctuators[key] = cancellable
    }

    override fun close() {
        // Cancel all scheduled punctuators
        punctuators.values.forEach { it.cancel() }
        punctuators.clear()
    }
}