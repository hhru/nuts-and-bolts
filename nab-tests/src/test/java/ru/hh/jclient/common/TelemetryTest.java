package ru.hh.jclient.common;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.inject.Inject;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.hh.jclient.common.util.storage.SingletonStorage;
import ru.hh.nab.hibernate.HibernateTestConfig;
import ru.hh.nab.jclient.NabJClientConfig;
import ru.hh.nab.jclient.UriCompactionUtil;
import ru.hh.nab.telemetry.IdGeneratorImpl;
import ru.hh.nab.telemetry.TelemetryListenerImpl;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {HibernateTestConfig.class, NabJClientConfig.class, TestConfig.class}
)
public class TelemetryTest {
  private static final TestRequestDebug DEBUG = new TestRequestDebug(true);
  private static final HttpClientContext HTTP_CLIENT_CONTEXT = new HttpClientContext(Collections.emptyMap(), Collections.emptyMap(),
      List.of(() -> DEBUG, requestDebug()));
  protected static SpanExporter spanExporter = spy(createSpanExporter());
  private static final OpenTelemetry openTelemetry = createOpenTelemetry();
  private static final TextMapPropagator textMapPropagator = textMapPropagator(openTelemetry);

  @Inject
  private List<HttpClientEventListener> eventListeners;

  private HttpClientFactory httpClientFactory;

  @BeforeEach
  public void beforeTest() {
    AsyncHttpClient httpClient = mock(AsyncHttpClient.class);
    when(httpClient.getConfig()).thenReturn(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(0).build());
    when(httpClient.executeRequest(isA(Request.class), isA(HttpClientImpl.CompletionHandler.class)))
        .then(iom -> {
          HttpClientImpl.CompletionHandler handler = iom.getArgument(1);
          handler.onCompleted(mock(Response.class));
          return null;
        });

    httpClientFactory = new HttpClientFactory(
        httpClient, Set.of(), new SingletonStorage<>(() -> HTTP_CLIENT_CONTEXT), Runnable::run, new DefaultRequestStrategy(), eventListeners);
  }

  @Test
  public void testSpanSending() throws InterruptedException, ExecutionException {
    ArgumentCaptor<Request> peopleCaptor = ArgumentCaptor.forClass(Request.class);
    httpClientFactory.with(new RequestBuilder().setUrl("http://test").build()).expectNoContent().result().get();
    verify(spanExporter, times(1)).export(any());
  }

  private static OpenTelemetry createOpenTelemetry() {
    SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
        .setIdGenerator(new IdGeneratorImpl(() -> HTTP_CLIENT_CONTEXT))
        .build();
    return OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .build();
  }

  private static TextMapPropagator textMapPropagator(OpenTelemetry openTelemetry) {
    return openTelemetry.getPropagators().getTextMapPropagator();

  }

  private static Supplier<RequestDebug> requestDebug() {
    return () -> new TelemetryListenerImpl(openTelemetry.getTracer("rt"), textMapPropagator,
        uri -> UriCompactionUtil.compactUri(uri, 1, 16));
  }

  private static SpanExporter createSpanExporter() {
    return new SpanExporter() {

      @Override
      public CompletableResultCode export(Collection<SpanData> spans) {
        return CompletableResultCode.ofSuccess();
      }

      @Override
      public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
      }

      @Override
      public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
      }
    };
  }
}
