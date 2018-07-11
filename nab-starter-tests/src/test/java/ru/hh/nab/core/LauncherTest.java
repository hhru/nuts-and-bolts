package ru.hh.nab.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.hh.nab.core.servlet.DefaultServletConfig;

@ContextConfiguration(classes = {CoreTestConfig.class})
public class LauncherTest extends AbstractJUnit4SpringContextTests {

  @Test
  public void startApplicationShouldRunJetty() throws Exception {
    assertNotNull(applicationContext);

    Launcher.startApplication(applicationContext, new DefaultServletConfig());

    assertEquals(CoreTestConfig.TEST_SERVICE_NAME, applicationContext.getBean("serviceName"));
  }
}
