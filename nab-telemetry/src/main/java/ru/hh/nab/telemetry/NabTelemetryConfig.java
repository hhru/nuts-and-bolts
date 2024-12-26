package ru.hh.nab.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import jakarta.inject.Named;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTER;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_VERSION;
import ru.hh.nab.common.servlet.ServletSystemFilterPriorities;
import ru.hh.nab.common.spring.boot.web.servlet.SystemFilterRegistrationBean;

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
  public SdkTracerProvider sdkTracerProvider(
      FileSettings fileSettings,
      IdGenerator idGenerator,
      @Named(SERVICE_NAME) String serviceName,
      @Named(SERVICE_VERSION) String serviceVersion,
      @Named(NODE_NAME) String nodeName,
      @Named(DATACENTER) String datacenter
  ) {
    boolean telemetryEnabled = fileSettings.getBoolean("opentelemetry.enabled", false);
    if (!telemetryEnabled) {
      return SdkTracerProvider.builder().build();
    } else {
      String url = fileSettings.getString("opentelemetry.collector.url");
      int timeout = fileSettings.getInteger("opentelemetry.export.timeoutMs", 10_000);
      int batchTimeout = fileSettings.getInteger("opentelemetry.export.batchTimeoutMs", 30_000);
      int batchDelay = fileSettings.getInteger("opentelemetry.export.batchDelayMs", 5000);
      int batchMaxSize = fileSettings.getInteger("opentelemetry.export.batchMaxSize", 512);
      int queueSize = fileSettings.getInteger("opentelemetry.export.queueSize", 2048);
      //1.0 - отправлять все спаны. 0.0 - ничего
      Double samplerRatio = fileSettings.getDouble("opentelemetry.sampler.ratio");
      if (url == null || url.isBlank()) {
        throw new IllegalStateException("'opentelemetry.collector.url' property can't be empty");
      }

      Resource serviceNameResource = Resource.create(
          Attributes
              .builder()
              .put(ResourceAttributes.SERVICE_NAME, serviceName)
              .put(ResourceAttributes.SERVICE_VERSION, serviceVersion)
              .put(ResourceAttributes.HOST_NAME, nodeName)
              .put(ResourceAttributes.CLOUD_REGION, datacenter)
              .build()
      );
      OtlpGrpcSpanExporter jaegerExporter = OtlpGrpcSpanExporter
          .builder()
          .setEndpoint(url)
          .setTimeout(timeout, TimeUnit.SECONDS)
          .build();
      BatchSpanProcessor spanProcessor = BatchSpanProcessor
          .builder(jaegerExporter)
          .setExporterTimeout(batchTimeout, TimeUnit.MILLISECONDS)
          .setScheduleDelay(batchDelay, TimeUnit.MILLISECONDS)
          .setMaxExportBatchSize(batchMaxSize)
          .setMaxQueueSize(queueSize)
          .build();

      SdkTracerProviderBuilder tracerProviderBuilder = SdkTracerProvider
          .builder()
          .addSpanProcessor(spanProcessor)
          .setResource(Resource.getDefault().merge(serviceNameResource))
          .setIdGenerator(idGenerator);

      if (samplerRatio != null) {
        tracerProviderBuilder.setSampler(Sampler.parentBased(Sampler.traceIdRatioBased(samplerRatio)));
      }

      return tracerProviderBuilder.build();
    }
  }

  @Bean
  TelemetryPropagator telemetryPropagator(OpenTelemetry openTelemetry) {
    return new TelemetryPropagator(openTelemetry);
  }

  @Bean
  SystemFilterRegistrationBean<TelemetryFilter> telemetryFilter(
      OpenTelemetry openTelemetry,
      TelemetryPropagator telemetryPropagator,
      FileSettings fileSettings
  ) {
    TelemetryFilter filter = new TelemetryFilter(
        openTelemetry.getTracer("nab"),
        telemetryPropagator,
        fileSettings.getBoolean("opentelemetry.enabled", false)
    );
    SystemFilterRegistrationBean<TelemetryFilter> registration = new SystemFilterRegistrationBean<>(filter);
    registration.setOrder(ServletSystemFilterPriorities.SYSTEM_OBSERVABILITY);
    return registration;
  }

  @Bean
  TelemetryProcessorFactory telemetryProcessorFactory(
      OpenTelemetry openTelemetry,
      TelemetryPropagator telemetryPropagator,
      HttpClientContextThreadLocalSupplier contextSupplier,
      FileSettings fileSettings
  ) {
    TelemetryProcessorFactory telemetryRequestDebug = new TelemetryProcessorFactory(
        openTelemetry.getTracer("jclient"),
        telemetryPropagator
    );
    if (fileSettings.getBoolean("opentelemetry.enabled", false)) {
      contextSupplier.register(new ContextStorage());
      contextSupplier.registerRequestDebugSupplier(telemetryRequestDebug::createRequestDebug);
    }
    return telemetryRequestDebug;
  }
}
