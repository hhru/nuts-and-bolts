package ru.hh.nab.telemetry;

import com.google.common.base.Strings;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
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
  public SdkTracerProvider sdkTracerProvider(FileSettings fileSettings, String serviceName, IdGenerator idGenerator) {
    String url = fileSettings.getString("opentelemetry.collector.url");
    boolean telemetryEnabled = fileSettings.getBoolean("opentelemetry.enabled", false);
    if (!telemetryEnabled) {
      return SdkTracerProvider.builder().build();
    } else {
      if (Strings.isNullOrEmpty(url)) {
        throw new IllegalStateException("'opentelemetry.collector.url' property can't be empty");
      }
      Resource serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName));

      ZipkinSpanExporter zipkinExporter = ZipkinSpanExporter.builder().setEndpoint(url).build();
      return SdkTracerProvider.builder()
          .addSpanProcessor(SimpleSpanProcessor.create(zipkinExporter))
          .setResource(Resource.getDefault().merge(serviceNameResource))
          .setIdGenerator(idGenerator)
          .build();
    }
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

  @Bean
  IdGenerator idGenerator(HttpClientContextThreadLocalSupplier httpClientContextSupplier) {
    return new IdGeneratorImpl(httpClientContextSupplier);
  }

  @Bean
  TelemetryProcessorFactory telemetryProcessorFactory(OpenTelemetry openTelemetry, HttpClientContextThreadLocalSupplier contextSupplier) {
    TelemetryProcessorFactory telemetryRequestDebug = new TelemetryProcessorFactory(openTelemetry.getTracer("jclient"),
        openTelemetry.getPropagators().getTextMapPropagator());
    contextSupplier.registerRequestDebugSupplier(telemetryRequestDebug::createRequestDebug);
    return telemetryRequestDebug;
  }

}
