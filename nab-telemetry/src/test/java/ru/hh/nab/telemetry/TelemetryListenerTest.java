package ru.hh.nab.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.internal.InternalAttributeKeyImpl;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.asynchttpclient.DefaultAsyncHttpClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.jclient.common.DefaultRequestStrategy;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.Uri;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.testbase.extensions.NabJunitWebConfig;
import ru.hh.nab.testbase.extensions.NabTestServer;
import ru.hh.nab.testbase.extensions.OverrideNabApplication;

@NabJunitWebConfig(NabTestConfig.class)
public class TelemetryListenerTest {
  private static final InMemorySpanExporter SPAN_EXPORTER = InMemorySpanExporter.create();

  private static OpenTelemetry telemetry;
  private static HttpClientContext httpClientContext;
  private static HttpClientFactory httpClientFactory;

  @NabTestServer(overrideApplication = SpringCtxForJersey.class)
  ResourceHelper resourceHelper;

  @BeforeAll
  public static void init() {
    SdkTracerProvider tracerProvider = SdkTracerProvider
        .builder()
        .addSpanProcessor(SimpleSpanProcessor.create(SPAN_EXPORTER))
        .build();

    telemetry = OpenTelemetrySdk
        .builder()
        .setTracerProvider(tracerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .build();

    TelemetryProcessorFactory telemetryProcessorFactory = new TelemetryProcessorFactory(
        telemetry.getTracer("jclient"), new TelemetryPropagator(telemetry));
    httpClientContext = new HttpClientContext(Collections.emptyMap(), Collections.emptyMap(), List.of());

    HttpClientContextThreadLocalSupplier contextSupplier = new HttpClientContextThreadLocalSupplier(() -> httpClientContext);
    contextSupplier.register(new ContextStorage());
    contextSupplier.registerRequestDebugSupplier(telemetryProcessorFactory::createRequestDebug);
    contextSupplier.addContext(Map.of(), Map.of());

    httpClientFactory = new HttpClientFactory(
        new DefaultAsyncHttpClient(), contextSupplier, Set.of(), Runnable::run, new DefaultRequestStrategy(), List.of());
  }

  @BeforeEach
  public void setUp() throws Exception {
    SPAN_EXPORTER.reset();
  }

  @Test
  public void testSimpleRequest() throws ExecutionException, InterruptedException {
    String url = resourceHelper.baseUrl() + "/simple";
    Response response = httpClientFactory.with(new RequestBuilder().setUrl(url).build()).unconverted().get();
    assertEquals("Hello, world!", response.getResponseBody());

    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);
    Attributes attributes = span.getAttributes();
    assertEquals(SpanKind.CLIENT, span.getKind());
    assertEquals("GET " + TelemetryListenerImpl.getNetloc(Uri.create(url)), span.getName());
    assertEquals("0000000000000000", span.getParentSpanId());
    assertEquals(url, attributes.get(SemanticAttributes.HTTP_URL));
    assertNull(attributes.get(InternalAttributeKeyImpl.create("http.request.cloud.region", AttributeType.STRING)));
    assertEquals("unknown", attributes.get(InternalAttributeKeyImpl.create("destination.address", AttributeType.STRING)));
  }

  @Test
  public void testErrorRequest() throws ExecutionException, InterruptedException {
    String url = resourceHelper.baseUrl() + "/error";
    Response response = httpClientFactory.with(new RequestBuilder().setUrl(url).build()).unconverted().get();
    assertEquals(500, response.getStatusCode());

    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);
    // StatusData раскладывается потом в otel.status_code и otel.status_description
    assertEquals("Server Error", span.getStatus().getDescription());
  }

  @Test
  public void testNestedRequestWithParent() throws ExecutionException, InterruptedException {
    String url = resourceHelper.baseUrl() + "/simple";

    Span parentSpan = telemetry
        .getTracer("test")
        .spanBuilder("parent")
        .setParent(Context.current())
        .setSpanKind(SpanKind.SERVER)
        .startSpan();

    try (Scope ignored = parentSpan.makeCurrent()) {
      Response response = httpClientFactory
          .with(new RequestBuilder().setUrl(url).build())
          .unconverted()
          .thenCompose(s -> httpClientFactory.with(new RequestBuilder().setUrl(url + "?a=1").build()).unconverted())
          .get();
      assertEquals("Hello, world!", response.getResponseBody());
    }

    parentSpan.end();

    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(3, spans.size());
    assertEquals(1, spans.stream().filter(data -> "parent".equals(data.getName())).count());

    List<SpanData> clientSpans = spans.stream().filter(data -> SpanKind.CLIENT.equals(data.getKind())).collect(Collectors.toList());
    assertEquals(2, clientSpans.stream().filter(data -> parentSpan.getSpanContext().getSpanId().equals(data.getParentSpanId())).count());
  }

  @Configuration
  @Import(TestResource.class)
  public static class SpringCtxForJersey implements OverrideNabApplication {
    @Override
    public NabApplication getNabApplication() {
      return NabApplication
          .builder()
          .configureJersey(SpringCtxForJersey.class)
          .bindToRoot()
          .build();
    }
  }
}
