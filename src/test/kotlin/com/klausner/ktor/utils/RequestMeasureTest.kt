package com.klausner.ktor.utils

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class RequestMeasureTest {

    @Test
    fun `when make a request than capture metrics`() = runBlocking {
        val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        val mockEngine = MockEngine { respond(content = "hello text", status = HttpStatusCode.OK) }

        val client = HttpClient(mockEngine) {
            install(KtorClientMetrics) { this@install.meterRegistry = meterRegistry }
        }
        client.get("http://localhost:8080")

        assertEquals("ktor.http.external.requests", meterRegistry.meters[0].id.name)
        assertEquals("http://localhost:8080", meterRegistry.meters[0].id.tags[0].value)
        assertEquals("GET", meterRegistry.meters[0].id.tags[1].value)
        assertEquals("/", meterRegistry.meters[0].id.tags[2].value)
        assertEquals("200", meterRegistry.meters[0].id.tags[3].value)
    }

}
