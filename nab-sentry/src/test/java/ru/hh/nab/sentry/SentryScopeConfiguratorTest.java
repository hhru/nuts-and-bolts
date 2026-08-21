package ru.hh.nab.sentry;

import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SentryScopeConfiguratorTest {

  private static final String TRACE_ID = "0123456789abcdef0123456789abcdef";

  @BeforeEach
  public void setUp() {
    Sentry.init(options -> options.setDsn("http://publickey@localhost/1"));
  }

  @AfterEach
  public void tearDown() {
    Sentry.close();
  }

  @Test
  public void testGetTraceIdReturnsWhatWasSet() {
    SentryScopeConfigurator.setTraceId(TRACE_ID);

    assertEquals(Optional.of(TRACE_ID), SentryScopeConfigurator.getTraceId());
  }

  /**
   * getTraceId() used to go through Sentry.getTraceparent(), which was dropped because it rebuilds and URL-encodes a
   * Baggage header on every call. This pins the cheap implementation to the same answer as the expensive one.
   */
  @Test
  public void testGetTraceIdMatchesTraceparent() {
    SentryScopeConfigurator.setTraceId(TRACE_ID);

    assertEquals(Sentry.getTraceparent().getTraceId().toString(), SentryScopeConfigurator.getTraceId().orElse(null));
  }

  @Test
  public void testGetTraceIdReturnsEmptyIdAfterClear() {
    SentryScopeConfigurator.setTraceId(TRACE_ID);
    SentryScopeConfigurator.clearTraceId();

    assertEquals(Optional.of(SentryId.EMPTY_ID.toString()), SentryScopeConfigurator.getTraceId());
  }
}
