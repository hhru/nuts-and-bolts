package ru.hh.nab.common.mdc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import static ru.hh.nab.common.mdc.MDC.CONTROLLER_MDC_KEY;
import static ru.hh.nab.common.mdc.MDC.REQUEST_ID_MDC_KEY;

public class MDCTest {

  @Test
  public void testSetKey() {
    MDC.setKey("testKey", "123");

    assertEquals("123", org.slf4j.MDC.get("testKey"));
  }

  @Test
  public void testGetController() {
    assertFalse(MDC.getController().isPresent());

    MDC.setKey(CONTROLLER_MDC_KEY, "123");

    assertEquals("123", MDC.getController().get());
  }

  @Test
  public void testGetRequestId() {
    assertFalse(MDC.getRequestId().isPresent());

    MDC.setKey(REQUEST_ID_MDC_KEY, "123");

    assertEquals("123", MDC.getRequestId().get());
  }
}
