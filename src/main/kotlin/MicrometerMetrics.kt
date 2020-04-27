import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.util.AttributeKey
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

class MicrometerMetrics(private val meterRegistry: MeterRegistry,
                        private val timerBuilder: Timer.Builder.(HttpClientCall) -> Unit ) {

    class Configuration {
        var meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        var timerBuilder: Timer.Builder.(HttpClientCall) -> Unit = { _ -> }
    }

    private fun Timer.Builder.customize(call: HttpClientCall) =
        this.apply { timerBuilder(call) }

    private fun RequestMeasure.recordDuration(call: HttpClientCall) {
        timer.stop(Timer.builder(requestTimerName)
            .addDefaultTags(call)
            .customize(call)
            .register(meterRegistry))
    }

    private fun Timer.Builder.addDefaultTags(call: HttpClientCall): Timer.Builder {
        tags(
            listOf(
                Tag.of("address", "${call.request.url.host}:${call.request.url.port}"),
                Tag.of("method", call.request.method.value),
                Tag.of("route", call.request.attributes[measureKey].route ?: call.request.url.encodedPath),
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


    companion object Feature : HttpClientFeature<Configuration, MicrometerMetrics> {

        private const val baseName = "ktor.http.external"
        const val requestTimerName = "$baseName.requests"
        private val measureKey: AttributeKey<RequestMeasure> = AttributeKey("metrics")
        override val key: AttributeKey<MicrometerMetrics> = AttributeKey("metrics")

        override fun install(feature: MicrometerMetrics, scope: HttpClient) {

            scope.sendPipeline.intercept(HttpSendPipeline.Before) {
                feature.before(context)
            }

            scope.receivePipeline.intercept(HttpReceivePipeline.After) {
                feature.after(context)
            }

        }

        override fun prepare(block: Configuration.() -> Unit): MicrometerMetrics {
            val config = Configuration().apply(block)
            return MicrometerMetrics(config.meterRegistry, config.timerBuilder)
        }
    }
}

private data class RequestMeasure(val timer: Timer.Sample,
                                  var route: String? = null,
                                  var throwable: Throwable? = null)
