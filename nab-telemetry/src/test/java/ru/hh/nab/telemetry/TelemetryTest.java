package ru.hh.nab.telemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.List;
import javax.ws.rs.core.Response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.testbase.extensions.NabJunitWebConfig;
import ru.hh.nab.testbase.extensions.NabTestServer;
import ru.hh.nab.testbase.extensions.OverrideNabApplication;

@NabJunitWebConfig(NabTestConfig.class)
public class TelemetryTest {
  private static final InMemorySpanExporter SPAN_EXPORTER = InMemorySpanExporter.create();

  @NabTestServer(overrideApplication = SpringCtxForJersey.class)
  ResourceHelper resourceHelper;

  @BeforeEach
  public void setUp() throws Exception {
    SPAN_EXPORTER.reset();
  }

  @Test
  public void testStatusIgnored() {
    Response response = resourceHelper
        .target("/status")
        .request()
        .get();

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(0, spans.size());
  }

  @Test
  public void testSimpleRequest() {
    Response response = resourceHelper
        .target("/simple")
        .request()
        .get();

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Hello, world!", response.readEntity(String.class));
    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);
    Attributes attributes = span.getAttributes();
    assertEquals(SpanKind.SERVER, span.getKind());
    assertEquals("TestResource#simple", span.getName());
    assertEquals("0000000000000000", span.getParentSpanId());
    assertEquals("/simple", attributes.get(SemanticAttributes.HTTP_TARGET));
    assertEquals(200, attributes.get(SemanticAttributes.HTTP_STATUS_CODE));
    assertEquals("GET", attributes.get(SemanticAttributes.HTTP_METHOD));
    assertEquals("127.0.0.1", attributes.get(SemanticAttributes.HTTP_HOST));
    assertEquals("simple", attributes.get(SemanticAttributes.CODE_FUNCTION));
    assertEquals("ru.hh.nab.telemetry.TestResource", attributes.get(SemanticAttributes.CODE_NAMESPACE));
  }

  @Test
  public void testRequestWithParentSpan() {
    Response response = resourceHelper
        .target("/simple")
        .request()
        .header("Traceparent", "00-1641597707000dfd4c0f1f07ea6cc943-fcf9c5cc0345247a-01")
        .get();

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Hello, world!", response.readEntity(String.class));
    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);
    assertEquals(SpanKind.SERVER, span.getKind());
    assertEquals("fcf9c5cc0345247a", span.getParentSpanId());
  }

  @Configuration
  @Import(TestResource.class)
  public static class SpringCtxForJersey implements OverrideNabApplication {
    @Override
    public NabApplication getNabApplication() {
      SdkTracerProviderBuilder tracerProviderBuilder = SdkTracerProvider
          .builder()
          .addSpanProcessor(SimpleSpanProcessor.create(SPAN_EXPORTER))
          .setResource(Resource.getDefault());

      OpenTelemetrySdkBuilder openTelemetrySdkBuilder = OpenTelemetrySdk.builder();
      openTelemetrySdkBuilder.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()));
      openTelemetrySdkBuilder.setTracerProvider(tracerProviderBuilder.build());
      OpenTelemetrySdk openTelemetry = openTelemetrySdkBuilder.buildAndRegisterGlobal();

      TelemetryFilter telemetryFilter = new TelemetryFilter(
          openTelemetry.getTracer("nab"),
          new TelemetryPropagator(openTelemetry),
          true);

      return NabApplication
          .builder()
          .addFilter(telemetryFilter)
          .bindToRoot()
          .configureJersey(SpringCtxForJersey.class)
          .bindToRoot()
          .build();
    }
  }
}
