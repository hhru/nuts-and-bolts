package ru.hh.nab.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.common.properties.FileSettings;

@Configuration
public class NabTelemetryConfig {

  @Bean
  public OpenTelemetry telemetry(FileSettings fileSettings, SdkTracerProvider tracerProvider) {
    OpenTelemetrySdkBuilder openTelemetrySdkBuilder = OpenTelemetrySdk.builder();
    openTelemetrySdkBuilder.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()));

    if (fileSettings.getBoolean("opentelemetry.enabled", false)) {
      openTelemetrySdkBuilder.setTracerProvider(tracerProvider);
    }
//    OpenTelemetrySdk openTelemetrySdk = openTelemetrySdkBuilder.buildAndRegisterGlobal(); //todo
    return openTelemetrySdkBuilder.build();
  }

  @Bean(destroyMethod = "shutdown")
  public SdkTracerProvider sdkTracerProvider(FileSettings fileSettings, String serviceName) {
    String url = fileSettings.getString("opentelemetry.collector.url", "http://localhost:9411/api/v2/spans");
    Resource serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName));

    ZipkinSpanExporter zipkinExporter = ZipkinSpanExporter.builder().setEndpoint(url).build();
    return SdkTracerProvider.builder()
        .addSpanProcessor(SimpleSpanProcessor.create(zipkinExporter))
        .setResource(Resource.getDefault().merge(serviceNameResource))
        .build();
  }

  @Bean
  TelemetryPropagator telemetryPropagator(OpenTelemetry openTelemetry) {
    return new TelemetryPropagator(openTelemetry);
  }

  @Bean
  TelemetryFilter telemetryFilter(OpenTelemetry openTelemetry, TelemetryPropagator telemetryPropagator) {
    Tracer tracer = openTelemetry.getTracer("nab");
    return new TelemetryFilter(tracer, telemetryPropagator);
  }


}
