package ru.hh.nab.common.mdc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

public class MDCTest {

  @Test
  public void testSetGetClearKey() {
    MDC.setKey("testKey", "123");

    assertEquals("123", org.slf4j.MDC.get("testKey"));
    assertEquals("123", MDC.getKey("testKey").get());

    MDC.deleteKey("testKey");

    assertNull(org.slf4j.MDC.get("testKey"));
    assertFalse(MDC.getKey("testKey").isPresent());
  }

  @Test
  public void testGetController() {
    MDC.setController("123");

    assertEquals("123", MDC.getController().get());

    MDC.clearController();

    assertFalse(MDC.getController().isPresent());
  }

  @Test
  public void testGetRequestId() {
    MDC.setRequestId("123");

    assertEquals("123", MDC.getRequestId().get());

    MDC.clearRequestId();

    assertFalse(MDC.getRequestId().isPresent());
  }
}
