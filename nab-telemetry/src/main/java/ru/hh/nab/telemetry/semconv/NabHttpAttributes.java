package ru.hh.nab.telemetry.semconv;

import io.opentelemetry.api.common.AttributeKey;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

public class NabHttpAttributes {

  public static final AttributeKey<String> HTTP_REQUEST_ORIGINAL_TIMEOUT = stringKey("http.request.original.timeout");
  public static final AttributeKey<Long> HTTP_REQUEST_TIMEOUT = longKey("http.request.timeout");

  private NabHttpAttributes() {}
}
