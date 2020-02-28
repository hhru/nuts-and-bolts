package ru.hh.nab.common;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.common.executor.MonitoredThreadPoolExecutor;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;

import java.util.Properties;

@ContextConfiguration(classes = {NabTestConfig.class})
public class MonitoredThreadPoolTest extends NabTestBase {

  private StatsDSender statsDSender;

  @Before
  public void setUp() {
    statsDSender = applicationContext.getBean(StatsDSender.class);
  }

  @Test
  public void testLongTaskDurationLogging() {
    Properties properties = new Properties();
    properties.put("longTaskDurationMs", 2);

    MonitoredThreadPoolExecutor executor = (MonitoredThreadPoolExecutor) MonitoredThreadPoolExecutor.create(
        new FileSettings(properties), "test", statsDSender, "test"
    );

    executor.execute(() -> {
      try {
        Thread.sleep(3);
      } catch (InterruptedException ignored) {
      }
    });
  }
}
