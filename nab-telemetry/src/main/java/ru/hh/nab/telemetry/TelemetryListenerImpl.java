package ru.hh.nab.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.PEER_SERVICE;
import jakarta.annotation.Nullable;
import static java.util.Optional.ofNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.HttpHeaderNames;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestContext;
import ru.hh.jclient.common.Uri;

public class TelemetryListenerImpl implements HttpClientEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryListenerImpl.class);
  private static final String UNKNOWN = "unknown";

  private final Tracer tracer;
  private final TelemetryPropagator telemetryPropagator;
  private Span span;

  public TelemetryListenerImpl(Tracer tracer, TelemetryPropagator telemetryPropagator) {
    this.tracer = tracer;
    this.telemetryPropagator = telemetryPropagator;
  }

  @Override
  public void onRequest(Request request, @Nullable Object requestBodyEntity, RequestContext context) {
    processRequest(request, context);
  }

  @Override
  public void onRetry(Request request, @Nullable Object requestBodyEntity, int retryCount, RequestContext context) {
    processRequest(request, context);
  }

  private void processRequest(Request request, RequestContext context) {
    if (span != null) {
      LOGGER.error("span already exist for {}", request.getUri());
      return;
    }

    String host = context.getUpstreamName() == null ? getNetloc(request.getUri()) : context.getUpstreamName();
    SpanBuilder builder = tracer
        .spanBuilder(request.getMethod() + " " + host)
        .setParent(Context.current())
        .setSpanKind(SpanKind.CLIENT)
        .setAttribute(HTTP_URL, request.getUrl())
        .setAttribute(HTTP_METHOD, request.getMethod())
        .setAttribute("http.request.timeout", request.getRequestTimeout())
        .setAttribute("http.request.original.timeout", ofNullable(request.getHeaders().get(HttpHeaderNames.X_OUTER_TIMEOUT_MS)).orElse("-1"))
        .setAttribute(PEER_SERVICE, host);

    builder.setAttribute("http.request.cloud.region", context.getDestinationDatacenter());
    builder.setAttribute(
        "destination.address",
        context.getDestinationHost() == null ? getExactUriHost(request.getUri()) : context.getDestinationHost()
    );

    span = builder.startSpan();
    LOGGER.trace("span started : {}", span);

    if (request.isExternalRequest()) {
      return;
    }

    try (Scope ignore = span.makeCurrent()) {
      telemetryPropagator.propagate(request);
    }
  }

  @Override
  public ru.hh.jclient.common.Response onResponse(ru.hh.jclient.common.Response response) {
    if (span == null) {
      LOGGER.error("span not exist for {}", response.getUri());
      return response;
    }

    StatusCode otelStatus = TelemetryPropagator.getStatus(response.getStatusCode(), false);
    span.setStatus(otelStatus, StatusCode.ERROR == otelStatus ? response.getStatusText() : "");
    span.setAttribute(HTTP_STATUS_CODE, response.getStatusCode());
    span.end();

    LOGGER.trace("span closed: {}", span);

    span = null;
    return response;
  }

  @Override
  public void onClientProblem(Throwable t) {
    if (span == null) {
      return;
    }

    span.setStatus(StatusCode.ERROR, t.getMessage());
    span.end();
    LOGGER.trace("span closed: {}", span);
  }

  public static String getNetloc(Uri uri) {
    return uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
  }

  private String getExactUriHost(Uri uri) {
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      return UNKNOWN;
    }
    return host;
  }
}
