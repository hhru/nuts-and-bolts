package ru.hh.nab.common.executor;

import org.junit.Test;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.common.properties.FileSettings;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class MonitoringThreadPoolExecutorTest {
  @Test
  public void testRejecting() {
    var properties = new Properties();
    properties.setProperty("minSize", "4");
    properties.setProperty("maxSize", "4");
    properties.setProperty("queueSize", "4");

    var tpe = new MonitoringThreadPoolExecutor("test", "test", new FileSettings(properties), mock(StatsDSender.class));

    tpe.execute(TASK);
    tpe.execute(TASK);
    tpe.execute(TASK);
    tpe.execute(TASK);

    var rejected = false;

    try {
      IntStream.range(0, 5).forEach(i -> tpe.execute(TASK));
      fail("RejectedExecutionException not thrown");
    } catch (RejectedExecutionException e) {
      rejected = true;
    }

    assertTrue(rejected);
  }

  private static Runnable TASK = () -> {
    try {
      new CountDownLatch(1).await();
    } catch (InterruptedException e) {
      //
    }
  };
}
