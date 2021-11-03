package ru.hh.nab.telemetry;

import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
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
    List<String> requestIdHolder = getRequestIdHolder();
    if (requestIdHolder == null || requestIdHolder.isEmpty()) {
      LOGGER.debug("unavailable requestId");
      return IdGenerator.random().generateTraceId();
    } else {
      String requestId = requestIdHolder.get(0);
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
    return Optional.ofNullable(contextSupplier.get())
        .map(context -> context.getHeaders().get(HttpHeaderNames.X_REQUEST_ID))
        .orElse(null);
  }
}
