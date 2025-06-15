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
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.glassfish.jersey.server.ResourceConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.jclient.common.DefaultRequestStrategy;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.Uri;
import ru.hh.trace.TraceContext;

@SpringBootTest(classes = TelemetryListenerTest.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TelemetryListenerTest {
  private static final InMemorySpanExporter SPAN_EXPORTER = InMemorySpanExporter.create();

  private static OpenTelemetry telemetry;
  private static HttpClientContext httpClientContext;
  private static HttpClientFactory httpClientFactory;

  @Inject
  private TestRestTemplate testRestTemplate;

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
        telemetry.getTracer("jclient"),
        new TelemetryPropagator(telemetry)
    );
    httpClientContext = new HttpClientContext(Collections.emptyMap(), Collections.emptyMap(), List.of());

    HttpClientContextThreadLocalSupplier contextSupplier = new HttpClientContextThreadLocalSupplier(() -> httpClientContext);
    contextSupplier.register(new ContextStorage());
    contextSupplier.registerEventListenerSupplier(telemetryProcessorFactory::createHttpClientEventListener);
    contextSupplier.addContext(Map.of(), Map.of());

    httpClientFactory = new HttpClientFactory(
        new DefaultAsyncHttpClient(), contextSupplier, Set.of(), Runnable::run, new DefaultRequestStrategy(), mock(TraceContext.class));
  }

  @BeforeEach
  public void setUp() {
    SPAN_EXPORTER.reset();
  }

  @Test
  public void testSimpleRequest() throws ExecutionException, InterruptedException {
    String url = testRestTemplate.getRootUri() + "/simple";
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
    assertEquals("localhost", attributes.get(InternalAttributeKeyImpl.create("destination.address", AttributeType.STRING)));
  }

  @Test
  public void testErrorRequest() throws ExecutionException, InterruptedException {
    String url = testRestTemplate.getRootUri() + "/error";
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
    String url = testRestTemplate.getRootUri() + "/simple";

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
  @ImportAutoConfiguration({
      ServletWebServerFactoryAutoConfiguration.class,
      JerseyAutoConfiguration.class,
  })
  public static class TestConfiguration {

    @Bean
    public ResourceConfig resourceConfig() {
      ResourceConfig resourceConfig = new ResourceConfig();
      resourceConfig.register(TestResource.class);
      return resourceConfig;
    }
  }
}
