package ru.hh.nab.telemetry;

import io.opentelemetry.api.common.AttributeKey;
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
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.springframework.http.RequestEntity.get;
import static org.springframework.http.RequestEntity.head;
import org.springframework.http.ResponseEntity;
import ru.hh.nab.common.servlet.ServletSystemFilterPriorities;
import ru.hh.nab.common.spring.boot.web.servlet.SystemFilterRegistrationBean;
import ru.hh.nab.web.jersey.filter.ResourceInformationFilter;
import ru.hh.nab.web.resource.StatusResource;

@SpringBootTest(classes = TelemetryTest.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TelemetryTest {
  private static final InMemorySpanExporter SPAN_EXPORTER = InMemorySpanExporter.create();

  @Inject
  private TestRestTemplate testRestTemplate;

  @BeforeEach
  public void setUp() {
    SPAN_EXPORTER.reset();
  }

  @Test
  public void testStatusIgnored() {
    ResponseEntity<String> response = testRestTemplate.getForEntity("/status", String.class);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(0, spans.size());
  }

  @Test
  public void testSimpleRequest() {
    String userAgent = "my-test-service";
    ResponseEntity<String> response = testRestTemplate.exchange(
        get("/simple").header("User-Agent", userAgent).build(),
        String.class
    );

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    assertEquals("Hello, world!", response.getBody());
    awaitAtLeastOneSpan();

    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);
    Attributes attributes = span.getAttributes();
    assertEquals(SpanKind.SERVER, span.getKind());
    assertEquals("TestResource#simple", span.getName());
    assertEquals("0000000000000000", span.getParentSpanId());
    assertEquals("/simple", attributes.get(SemanticAttributes.HTTP_TARGET));
    assertEquals("/simple", attributes.get(SemanticAttributes.HTTP_ROUTE));
    assertEquals(200, attributes.get(SemanticAttributes.HTTP_STATUS_CODE));
    assertEquals("GET", attributes.get(SemanticAttributes.HTTP_METHOD));
    assertEquals("localhost", attributes.get(SemanticAttributes.HTTP_HOST));
    assertEquals("simple", attributes.get(SemanticAttributes.CODE_FUNCTION));
    assertEquals("ru.hh.nab.telemetry.TestResource", attributes.get(SemanticAttributes.CODE_NAMESPACE));
    assertEquals(userAgent, attributes.get(AttributeKey.stringKey("user_agent.original")));
  }

  @Test
  public void testRouteCalculation() {
    String template = "/simple/{name}/greeting";
    ResponseEntity<String> response = testRestTemplate.getForEntity(template, String.class, "telemetry");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    awaitAtLeastOneSpan();

    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);
    Attributes attributes = span.getAttributes();
    assertEquals(template, attributes.get(SemanticAttributes.HTTP_ROUTE));
  }

  @Test
  public void testRouteCalculationForParentThatHasSubresource() {
    String template = "/resource/simple/{name}/greeting";
    ResponseEntity<String> response = testRestTemplate.getForEntity(template, String.class, "telemetry");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    awaitAtLeastOneSpan();

    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);
    Attributes attributes = span.getAttributes();
    assertEquals(template, attributes.get(SemanticAttributes.HTTP_ROUTE));
  }

  @Test
  public void testRouteCalculationForSubresource() {
    String template = "/resource/sub/simple/{name}/greeting";
    ResponseEntity<String> response = testRestTemplate.getForEntity(template, String.class, "telemetry");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    awaitAtLeastOneSpan();

    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);
    Attributes attributes = span.getAttributes();
    assertEquals(template, attributes.get(SemanticAttributes.HTTP_ROUTE));
  }

  @Test
  public void testRouteCalculationWithBasePath() {
    String template = "/app/resource/sub/simple/{name}/greeting";
    ResponseEntity<String> response = testRestTemplate.getForEntity(template, String.class, "telemetry");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    awaitAtLeastOneSpan();

    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);
    Attributes attributes = span.getAttributes();
    assertEquals(template, attributes.get(SemanticAttributes.HTTP_ROUTE));
  }

  @Test
  public void testRouteWithDuplicatedPath() {
    String template = "/resource/sub/simple";
    ResponseEntity<String> response = testRestTemplate.exchange(head(template).build(), String.class);

    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatusCode().value());
    awaitAtLeastOneSpan();

    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);
    Attributes attributes = span.getAttributes();
    assertEquals(template, attributes.get(SemanticAttributes.HTTP_ROUTE));
  }

  @Test
  public void testRouteCalculatedForRoot() {
    String template = "/";
    ResponseEntity<String> response = testRestTemplate.exchange(head(template).build(), String.class);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    awaitAtLeastOneSpan();

    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);
    Attributes attributes = span.getAttributes();
    assertEquals(template, attributes.get(SemanticAttributes.HTTP_ROUTE));
  }

  @Test
  public void testRequestWithParentSpan() {
    ResponseEntity<String> response = testRestTemplate.exchange(
        get("/simple").header("Traceparent", "00-1641597707000dfd4c0f1f07ea6cc943-fcf9c5cc0345247a-01").build(),
        String.class
    );

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    assertEquals("Hello, world!", response.getBody());
    awaitAtLeastOneSpan();

    List<SpanData> spans = SPAN_EXPORTER.getFinishedSpanItems();
    assertEquals(1, spans.size());
    SpanData span = spans.get(0);
    assertEquals(SpanKind.SERVER, span.getKind());
    assertEquals("fcf9c5cc0345247a", span.getParentSpanId());
  }

  private void awaitAtLeastOneSpan() {
    await().atMost(1000, TimeUnit.MILLISECONDS).untilAsserted(() -> assertFalse(SPAN_EXPORTER.getFinishedSpanItems().isEmpty()));
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
      resourceConfig.register(TestResourceWithSubResource.class);
      resourceConfig.register(TestResourceWithSubResource.SubResource.class);
      resourceConfig.register(ResourceInformationFilter.class);
      return resourceConfig;
    }

    @Bean
    public ServletRegistrationBean<ServletContainer> statusServletRegistration() {
      StatusResource statusResource = new StatusResource("", "", () -> Duration.ofSeconds(5));
      return new ServletRegistrationBean<>(new ServletContainer(new ResourceConfig().register(statusResource)), "/status");
    }

    @Bean
    public ServletRegistrationBean<ServletContainer> jerseyServletRegistration(ResourceConfig resourceConfig) {
      return new ServletRegistrationBean<>(new ServletContainer(resourceConfig), "/*", "/app/*");
    }

    @Bean
    public SystemFilterRegistrationBean<TelemetryFilter> telemetryFilter() {
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
          true
      );

      SystemFilterRegistrationBean<TelemetryFilter> registration = new SystemFilterRegistrationBean<>(telemetryFilter);
      registration.setOrder(ServletSystemFilterPriorities.SYSTEM_OBSERVABILITY);
      return registration;
    }
  }
}
