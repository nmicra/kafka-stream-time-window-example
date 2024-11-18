package com.github.nmicra.time_window_streams.domain

data class Order(
    val id: String,
    val version: Int,
    val orderDetails: String
)