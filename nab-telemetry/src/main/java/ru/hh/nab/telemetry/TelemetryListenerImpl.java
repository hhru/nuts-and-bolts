package ru.hh.nab.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.incubating.DestinationIncubatingAttributes;
import io.opentelemetry.semconv.incubating.PeerIncubatingAttributes;
import jakarta.annotation.Nullable;
import static java.util.Optional.ofNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.HttpHeaderNames;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestContext;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.Uri;
import ru.hh.nab.telemetry.semconv.NabHttpAttributes;
import ru.hh.nab.telemetry.semconv.NabPeerAttributes;
import ru.hh.nab.telemetry.semconv.SemanticAttributesForRemoval;

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

        .setAttribute(SemanticAttributesForRemoval.HTTP_URL, request.getUrl())
        .setAttribute(UrlAttributes.URL_FULL, request.getUrl())

        .setAttribute(SemanticAttributesForRemoval.HTTP_METHOD, request.getMethod())
        .setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, request.getMethod())

        .setAttribute(NabHttpAttributes.HTTP_REQUEST_TIMEOUT, request.getRequestTimeout())
        .setAttribute(
            NabHttpAttributes.HTTP_REQUEST_ORIGINAL_TIMEOUT,
            ofNullable(request.getHeaders().get(HttpHeaderNames.X_OUTER_TIMEOUT_MS)).orElse("-1")
        )
        .setAttribute(PeerIncubatingAttributes.PEER_SERVICE, host);

    builder.setAttribute(SemanticAttributesForRemoval.HTTP_REQUEST_CLOUD_REGION, context.getDestinationDatacenter());
    builder.setAttribute(NabPeerAttributes.PEER_CLOUD_AVAILABILITY_ZONE, context.getDestinationDatacenter());

    builder.setAttribute(
        DestinationIncubatingAttributes.DESTINATION_ADDRESS,
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
  public Response onResponse(Response response) {
    if (span == null) {
      LOGGER.error("span not exist for {}", response.getUri());
      return response;
    }

    StatusCode otelStatus = TelemetryPropagator.getStatus(response.getStatusCode(), false);
    span.setStatus(otelStatus, StatusCode.ERROR == otelStatus ? response.getStatusText() : "");

    span.setAttribute(SemanticAttributesForRemoval.HTTP_STATUS_CODE, response.getStatusCode());
    span.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, response.getStatusCode());

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
