package ru.hh.nab.telemetry;

import com.google.common.base.Strings;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.concurrent.TimeUnit;
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
    return openTelemetrySdkBuilder.buildAndRegisterGlobal();
  }

  @Bean
  IdGenerator idGenerator(HttpClientContextThreadLocalSupplier httpClientContextSupplier) {
    return new IdGeneratorImpl(httpClientContextSupplier);
  }

  @Bean(destroyMethod = "shutdown")
  public SdkTracerProvider sdkTracerProvider(FileSettings fileSettings, String serviceName, IdGenerator idGenerator) {
    boolean telemetryEnabled = fileSettings.getBoolean("opentelemetry.enabled", false);
    if (!telemetryEnabled) {
      return SdkTracerProvider.builder().build();
    } else {
      String url = fileSettings.getString("opentelemetry.collector.host");
      int port = fileSettings.getInteger("opentelemetry.collector.port");
      if (Strings.isNullOrEmpty(url)) {
        throw new IllegalStateException("'opentelemetry.collector.host' property can't be empty");
      }

      Resource serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName));
      ManagedChannel jaegerChannel = ManagedChannelBuilder.forAddress(url, port).usePlaintext().build();
      JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
          .setChannel(jaegerChannel)
          .setTimeout(30, TimeUnit.SECONDS)
          .build();

      return SdkTracerProvider.builder()
          .addSpanProcessor(SimpleSpanProcessor.create(jaegerExporter))
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
  TelemetryFilter telemetryFilter(OpenTelemetry openTelemetry, TelemetryPropagator telemetryPropagator, FileSettings fileSettings) {
    Tracer tracer = openTelemetry.getTracer("nab");
    return new TelemetryFilter(tracer, telemetryPropagator, fileSettings.getBoolean("opentelemetry.enabled", false));
  }

  @Bean
  TelemetryProcessorFactory telemetryProcessorFactory(OpenTelemetry openTelemetry, HttpClientContextThreadLocalSupplier contextSupplier,
                                                      FileSettings fileSettings) {
    TelemetryProcessorFactory telemetryRequestDebug = new TelemetryProcessorFactory(openTelemetry.getTracer("jclient"),
        openTelemetry.getPropagators().getTextMapPropagator());
    if (fileSettings.getBoolean("opentelemetry.enabled", false)) {
      contextSupplier.registerRequestDebugSupplier(telemetryRequestDebug::createRequestDebug);
    }
    return telemetryRequestDebug;
  }

}
