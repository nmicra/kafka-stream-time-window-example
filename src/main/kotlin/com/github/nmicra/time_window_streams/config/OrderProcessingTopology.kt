package com.github.nmicra.time_window_streams.config

import com.github.nmicra.time_window_streams.domain.Order
import com.github.nmicra.time_window_streams.domain.OrderState
import com.github.nmicra.time_window_streams.processor.OrderTransformer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.serializer.JsonSerde

@Configuration
class OrderProcessingTopology {

    private val stateStoreName = "order-state-store"

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun kStream(builder: StreamsBuilder): KStream<String, Order> {
        val orderSerde = JsonSerde(Order::class.java)
        val orderStateSerde = JsonSerde(OrderState::class.java)
        val stringSerde = Serdes.String()

        // Define the state store
        val storeBuilder = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(stateStoreName),
            stringSerde,
            orderStateSerde // Use the correct SerDe here
        )
        builder.addStateStore(storeBuilder)

        // Consume orders from the "ordering" topic
        val ordersStream: KStream<String, Order> = builder.stream("ordering", Consumed.with(stringSerde, orderSerde))

        // Apply the custom transformer using TransformerSupplier
        ordersStream
            .peek { key, value -> logger.debug(">>> Consumed message with key: $key, value: $value") }
            .transform(
            { OrderTransformer(stateStoreName) }, // TransformerSupplier lambda
            stateStoreName
        )
            .to("processing", Produced.with(stringSerde, orderSerde))

        return ordersStream
    }
}