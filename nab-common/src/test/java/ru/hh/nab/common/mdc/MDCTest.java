package ru.hh.nab.common.mdc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

  @Test
  public void testGenerateRequestId() {
    assertTrue(MDC.generateRequestId("_test").endsWith("_test"));
  }

  @Test
  public void testRunWithRequestId() {
    MDC.setRequestId("123");

    MDC.runWithRequestId("456", () -> {
      assertEquals("456", MDC.getRequestId().get());
      return null;
    });

    assertEquals("123", MDC.getRequestId().get());

    MDC.clearRequestId();
  }
}
