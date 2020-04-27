# KTOR CLIENT PROMETHEUS
## Description
Prometheus metrics for ktor client.

## How to use
```
val httpClient = HttpClient(CIO) {
    install(MicrometerMetrics) {
        meterRegister = PrometheusMeterRegistry(PrometheusMeterRegistry.DEFAULT)
    }
}
```
