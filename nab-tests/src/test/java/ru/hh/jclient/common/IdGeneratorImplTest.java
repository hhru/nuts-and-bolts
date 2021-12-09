package ru.hh.jclient.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;
import ru.hh.nab.telemetry.IdGeneratorImpl;

public class IdGeneratorImplTest {


  private static final String VALID_REQUEST_ID = "163897206709842601f90a070699ac44";

  @Test
  public void testGenerateTraceIdWithNormalRequestId() {
    IdGeneratorImpl idGenerator = getIdGeneratorWithRequestId(VALID_REQUEST_ID);

    var traceId = idGenerator.generateTraceId();

    assertEquals(VALID_REQUEST_ID, traceId);
  }

  @Test
  public void testGenerateTraceIdWithHexString() {
    IdGeneratorImpl idGenerator = getIdGeneratorWithRequestId(VALID_REQUEST_ID + "_some_postfix_string");

    var traceId = idGenerator.generateTraceId();

    assertEquals(VALID_REQUEST_ID, traceId);
  }

  @Test
  public void testGenerateTraceIdWithShortRequestId() {
    IdGeneratorImpl idGenerator = getIdGeneratorWithRequestId("163897206");

    var traceId = idGenerator.generateTraceId();

    assertEquals(32, traceId.length());
  }

  @Test
  public void testGenerateTraceIdWithNoHexRequestId() {
    IdGeneratorImpl idGenerator = getIdGeneratorWithRequestId("16389720670_NOT_HEX_9842601f90a070699ac44_some_postfix_string");

    var traceId = idGenerator.generateTraceId();

    assertEquals(32, traceId.length());
    assertNotEquals("16389720670_NOT_HEX_9842601f90a0", traceId);
  }

  @Test
  public void testGenerateTraceIdWithoutRequestId() {
    HttpClientContext httpClientContext = new HttpClientContext(
        Map.of(),
        Collections.emptyMap(),
        Collections.emptyList()
    );
    var idGenerator = new IdGeneratorImpl(() -> httpClientContext);

    var traceId = idGenerator.generateTraceId();

    assertEquals(32, traceId.length());
  }


  private IdGeneratorImpl getIdGeneratorWithRequestId(String requestId) {
    HttpClientContext httpClientContext = new HttpClientContext(
        Map.of(HttpHeaderNames.X_REQUEST_ID, List.of(requestId)),
        Collections.emptyMap(),
        Collections.emptyList()
    );
    return new IdGeneratorImpl(() -> httpClientContext);
  }
}
