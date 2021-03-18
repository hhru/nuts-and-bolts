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
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;
import ru.hh.jclient.common.util.storage.SingletonStorage;
import ru.hh.nab.telemetry.IdGeneratorImpl;
import ru.hh.nab.telemetry.TelemetryListenerImpl;

public class TelemetryBenchmark {
  private static final HttpClientContext HTTP_CLIENT_CONTEXT = new HttpClientContext(Collections.emptyMap(), Collections.emptyMap()
      , List.of(requestDebug())
//      ,List.of()
  );
  protected static SpanExporter spanExporter = spy(createSpanExporter());
  private static final OpenTelemetry openTelemetry = createOpenTelemetry();
  private static final TextMapPropagator textMapPropagator = textMapPropagator(openTelemetry);

  private static final HttpClientFactory httpClientFactory = httpclientFactory();

  public static void main(String[] args) throws Exception {
    org.openjdk.jmh.Main.main(args);
  }


  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
  @Warmup(iterations = 1, time = 1)
  public void testSpanSending() throws InterruptedException, ExecutionException {
    httpClientFactory.with(new RequestBuilder().setUrl("http://test").build()).expectNoContent().result().get();
  }

  public static HttpClientFactory httpclientFactory() {
    AsyncHttpClient httpClient = mock(AsyncHttpClient.class);
    when(httpClient.getConfig()).thenReturn(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(0).build());
    when(httpClient.executeRequest(isA(Request.class), isA(HttpClientImpl.CompletionHandler.class)))
        .then(iom -> {
          HttpClientImpl.CompletionHandler handler = iom.getArgument(1);
          handler.onCompleted(mock(Response.class));
          return null;
        });

    return new HttpClientFactory(
        httpClient, Set.of(), new SingletonStorage<>(() -> HTTP_CLIENT_CONTEXT), Runnable::run, new DefaultRequestStrategy(), List.of());
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
    return () -> new TelemetryListenerImpl(openTelemetry.getTracer("rt"), textMapPropagator);
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
