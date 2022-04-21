[![Java CI with Maven](https://github.com/klausner17/ktor-client-prometheus/actions/workflows/maven.yml/badge.svg)](https://github.com/klausner17/ktor-client-prometheus/actions/workflows/maven.yml)

# KTOR CLIENT PROMETHEUS
## Description
Prometheus metrics for ktor client.

## How to use

```kotlin
val httpClient = HttpClient(ENGINE) {
    install(KtorClientPrometheus) {
        meterRegister = PrometheusMeterRegistry(PrometheusMeterRegistry.DEFAULT)
    }
}

client.get("http://localhost:8080")
```

In some cases it's necessary get the endpoint replacement instead the endpoint url. For example:

```
http://localhost:8080/v1/cities/1
http://localhost:8080/v1/cities/2
```

but the metrics we want is

```
http://localhost:8080/v1/cities/{city_id}
```

So, to do this must be added an AttributeRoute in client call

```kotlin
client.get("http://localhost:8080/v1/cities/1") {
    attributes.put(AttributeRoute, "v1/cities/{city_id}")
}
```

