package ru.hh.nab.telemetry;

import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.sdk.trace.IdGenerator;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpHeaderNames;

public class IdGeneratorImpl implements IdGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(IdGeneratorImpl.class);

  private final Supplier<HttpClientContext> contextSupplier;

  public IdGeneratorImpl(Supplier<HttpClientContext> contextSupplier) {
    this.contextSupplier = contextSupplier;
  }

  @Override
  public String generateSpanId() {
    return IdGenerator.random().generateSpanId();
  }

  @Override
  public String generateTraceId() {
    // TODO: https://jira.hh.ru/browse/HH-233805
    List<String> requestIdHolder = getRequestIdHolder();
    if (requestIdHolder == null || requestIdHolder.isEmpty()) {
      LOGGER.debug("unavailable requestId");
      return IdGenerator.random().generateTraceId();
    } else if (requestIdHolder.get(0).length() < 32) {
      LOGGER.debug("invalid requestId = {} is less than 32 character", requestIdHolder.get(0));
      return IdGenerator.random().generateTraceId();
    } else {
      String requestId = requestIdHolder.get(0).substring(0, 32);
      if (!TraceId.isValid(requestId)) {
        LOGGER.debug("invalid requestId for telemetry {}", requestId);
        return IdGenerator.random().generateTraceId();
      } else {
        return requestId;
      }
    }
  }

  @Nullable
  private List<String> getRequestIdHolder() {
    return Optional
        .ofNullable(contextSupplier.get())
        .map(context -> context.getHeaders().get(HttpHeaderNames.X_REQUEST_ID))
        .orElse(null);
  }
}
