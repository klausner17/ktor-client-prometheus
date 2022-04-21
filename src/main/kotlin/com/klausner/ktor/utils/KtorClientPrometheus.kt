package com.klausner.ktor.utils

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.micrometer.core.instrument.*
import io.micrometer.prometheus.*

val AttributeRoute = AttributeKey<String>("route")

class KtorClientMetrics(private val meterRegistry: MeterRegistry,
                        private val timerBuilder: Timer.Builder.(HttpClientCall) -> Unit ) {

    class Configuration {
        var meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        var timerBuilder: Timer.Builder.(HttpClientCall) -> Unit = { _ -> }
    }

    private fun Timer.Builder.customize(call: HttpClientCall) = this.apply { timerBuilder(call) }

    private fun RequestMeasure.recordDuration(call: HttpClientCall) {
        timer.stop(
            Timer.builder(requestTimerName)
                .publishPercentiles(0.5, 0.9, 0.95, 0.99, 0.999)
                .addDefaultTags(call)
                .customize(call)
                .register(meterRegistry)
        )
    }

    private fun Timer.Builder.addDefaultTags(call: HttpClientCall): Timer.Builder {
        tags(
            listOf(
                Tag.of("address", "${call.request.url.protocol.name}://${call.request.url.hostWithPort}"),
                Tag.of("route", buildTagRoute(call)),
                Tag.of("method", call.request.method.value),
                Tag.of("status", call.response.status.value.toString())
            )
        )
        return this
    }

    private fun before(clientCall: HttpRequestBuilder) {
        clientCall.attributes.put(measureKey, RequestMeasure(Timer.start(meterRegistry)))
    }

    private fun after(clientCall: HttpClientCall) {
        clientCall.attributes.getOrNull(measureKey)?.recordDuration(clientCall)
    }

    private fun buildTagRoute(call: HttpClientCall): String {
        val route = call.request.attributes.getOrNull(AttributeRoute) ?: call.request.url.encodedPath
        return if (!route.startsWith("/")) { "/${route}" } else route
    }

    companion object Feature : HttpClientPlugin<Configuration, KtorClientMetrics> {

        private const val baseName = "ktor.http.external"
        const val requestTimerName = "$baseName.requests"
        private val measureKey: AttributeKey<RequestMeasure> = AttributeKey("metrics")
        override val key: AttributeKey<KtorClientMetrics> = AttributeKey("metrics")

        override fun install(feature: KtorClientMetrics, scope: HttpClient) {

            scope.sendPipeline.intercept(HttpSendPipeline.Before) {
                try {
                    feature.before(context)
                } finally {
                    proceed()
                }
            }

            scope.receivePipeline.intercept(HttpReceivePipeline.After) {
                try {
                    feature.after(this.subject.call)
                } finally {
                    proceed()
                }
            }
        }

        override fun prepare(block: Configuration.() -> Unit): KtorClientMetrics {
            val config = Configuration().apply(block)
            return KtorClientMetrics(config.meterRegistry, config.timerBuilder)
        }
    }
}

private data class RequestMeasure(
    val timer: Timer.Sample,
    var route: String? = null,
    var throwable: Throwable? = null
)
