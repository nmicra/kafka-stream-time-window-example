# Example of Time-Windowing with Kafka-Streams
You produce an order 
``
data class Order(
    val id: String,
    val version: Int,
    val orderDetails: String
)
``
into the `order` topic. The ID might be the same, but version is always increased.
There is one minute time-window, if several versions were produced into the `order` topic,
only with the highest version, will be moved into the `processing` window.
For each Order-ID, the time-window countdown is different. Starting when the first Order received. 