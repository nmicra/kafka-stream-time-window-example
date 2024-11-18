package com.github.nmicra.time_window_streams.domain

import java.time.Instant

data class OrderState(
    var highestVersionOrder: Order,
    var windowStartTime: Long
)