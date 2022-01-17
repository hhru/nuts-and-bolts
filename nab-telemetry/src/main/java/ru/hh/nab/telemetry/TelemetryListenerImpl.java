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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestContext;
import ru.hh.jclient.common.RequestDebug;
import ru.hh.jclient.common.Uri;
import ru.hh.jclient.common.exception.ResponseConverterException;

public class TelemetryListenerImpl implements RequestDebug {
  private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryListenerImpl.class);

  private final Tracer tracer;
  private final TelemetryPropagator telemetryPropagator;
  private Span span;

  public TelemetryListenerImpl(Tracer tracer, TelemetryPropagator telemetryPropagator) {
    this.tracer = tracer;
    this.telemetryPropagator = telemetryPropagator;
  }

  @Override
  public void onRequest(Request request, Optional<?> requestBodyEntity, RequestContext context) {
    String host = context.upstreamName == null ? getNetloc(request.getUri()) : context.upstreamName;
    SpanBuilder builder = tracer.spanBuilder(request.getMethod() + " " + host)
        .setParent(Context.current())
        .setSpanKind(SpanKind.CLIENT)
        .setAttribute(HTTP_URL, request.getUrl())
        .setAttribute(HTTP_METHOD, request.getMethod())
        .setAttribute("http.request.timeout", request.getRequestTimeout());

    if (context.datacenter != null) {
      builder.setAttribute("http.request.cloud.region", context.datacenter);
    }

    span = builder.startSpan();
    LOGGER.trace("spanStarted : {}", span);

    if (request.isExternalRequest()) {
      return;
    }

    try (Scope ignore = span.makeCurrent()) {
      telemetryPropagator.propagate(request);
    }
  }

  @Override
  public ru.hh.jclient.common.Response onResponse(ru.hh.jclient.common.Response response) {
    span.setStatus(TelemetryPropagator.getStatus(response.getStatusCode()));
    span.setAttribute(HTTP_STATUS_CODE, response.getStatusCode());
    span.end();
    LOGGER.trace("span closed: {}", span);
    return response;
  }

  @Override
  public void onClientProblem(Throwable t) {
    span.setStatus(StatusCode.ERROR, t.getMessage());
    span.end();
    LOGGER.trace("span closed: {}", span);
  }

  public static String getNetloc(Uri uri) {
    return uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
  }

  @Override
  public void onRetry(Request request, Optional<?> requestBodyEntity, int retryCount, RequestContext context) {

  }


  @Override
  public void onResponseConverted(Optional<?> result) {

  }


  @Override
  public void onConverterProblem(ResponseConverterException e) {

  }

  @Override
  public void onProcessingFinished() {

  }
}
