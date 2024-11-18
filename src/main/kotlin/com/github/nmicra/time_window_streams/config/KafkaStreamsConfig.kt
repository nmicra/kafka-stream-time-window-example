package com.github.nmicra.time_window_streams.config

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.support.serializer.JsonSerde


@Configuration
@EnableKafkaStreams
class KafkaStreamsConfig {

    @Bean(name = ["defaultKafkaStreamsConfig"])
    fun kStreamsConfigs(): KafkaStreamsConfiguration {
        val props = mutableMapOf<String, Any>(
            StreamsConfig.APPLICATION_ID_CONFIG to "order-processing-app",
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.String()::class.java.name,
            StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to JsonSerde::class.java.name,
            StreamsConfig.PROCESSING_GUARANTEE_CONFIG to StreamsConfig.EXACTLY_ONCE_V2,
            StreamsConfig.COMMIT_INTERVAL_MS_CONFIG to 1000
        )
        return KafkaStreamsConfiguration(props)
    }
}