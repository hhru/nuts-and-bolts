package ru.hh.nab.common.executor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.metrics.StatsDSender;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LoggerFactory.class})
public class MonitoredThreadPoolExecutorTest {
  @Test
  public void testRejecting() {
    var properties = new Properties();
    properties.setProperty("minSize", "4");
    properties.setProperty("maxSize", "4");

    var tpe = MonitoredThreadPoolExecutor.create(new FileSettings(properties), "test", mock(StatsDSender.class), "test");

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
    LATCH.countDown();
  }

  @Test
  public void testLoggingLongTaskDuration() {
    mockStatic(LoggerFactory.class);
    Logger mockedLogger = mock(Logger.class);
    when(LoggerFactory.getLogger(MonitoredThreadPoolExecutor.class)).thenReturn(mockedLogger);

    Properties properties = new Properties();
    properties.setProperty("longTaskDurationMs", "50");

    ThreadPoolExecutor executor = MonitoredThreadPoolExecutor.create(
        new FileSettings(properties), "test", mock(StatsDSender.class), "test"
    );
    CompletableFuture.runAsync(() -> {}, executor).join();

    verifyZeroInteractions(mockedLogger);

    CompletableFuture.runAsync(LONG_TASK, executor).join();

    verify(mockedLogger).warn(anyString(), anyString(), anyInt(), anyInt());
  }

  private static final CountDownLatch LATCH = new CountDownLatch(1);
  private static final Runnable TASK = () -> {
    try {
      LATCH.await();
    } catch (InterruptedException e) {
      //
    }
  };

  private static final Runnable LONG_TASK = () -> {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  };
}
