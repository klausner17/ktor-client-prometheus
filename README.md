# KTOR CLIENT PROMETHEUS
## Description
Prometheus metrics for ktor client.

## How to use
```
val httpClient = HttpClient(ENGINE) {
    install(KtorClientPrometheus) {
        meterRegister = PrometheusMeterRegistry(PrometheusMeterRegistry.DEFAULT)
    }
}
```
