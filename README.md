# KTOR CLIENT PROMETHEUS
## Description
Prometheus metrics for ktor client.

## How to use
``
val = HttpClient(CIO) {
    install(MicrometerMetrics) {
        meterRegister = PromethesMeterRegistry(PrometheusMeterRegistry.DEFAULT)
    }
}
``