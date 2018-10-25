package ru.hh.nab.starter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext;
import org.junit.Test;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.testbase.NabTestConfig;

public class NabApplicationTest {

  @Test
  public void runShouldStartJetty() {
    JettyServer server = NabApplication.runDefaultWebApp(NabTestConfig.class);

    assertTrue(server.isRunning());
    assertEquals(NabTestConfig.TEST_SERVICE_NAME, getWebApplicationContext(server.getServletContext()).getBean("serviceName"));
  }
}
