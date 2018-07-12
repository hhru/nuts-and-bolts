package ru.hh.nab.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class NabApplicationTest {

  @Test
  public void runShouldStartJetty() {
    NabApplicationContext context = (NabApplicationContext) NabApplication.run(CoreTestConfig.class);

    assertTrue(context.isServerRunning());
    assertEquals(CoreTestConfig.TEST_SERVICE_NAME, context.getBean("serviceName"));
  }
}
