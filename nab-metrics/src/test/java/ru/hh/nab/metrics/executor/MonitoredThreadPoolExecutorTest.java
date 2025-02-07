package ru.hh.nab.metrics.executor;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.slf4j.LoggerFactory;
import ru.hh.nab.metrics.StatsDSender;

public class MonitoredThreadPoolExecutorTest {

  private final ListAppender<ILoggingEvent> memoryAppender = new ListAppender<>();
  {
    memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
  }
  @Test
  public void testRejecting() {
    var properties = new Properties();
    properties.setProperty("minSize", "4");
    properties.setProperty("maxSize", "4");

    var tpe = MonitoredThreadPoolExecutor.create(
        properties,
        "test",
        mock(StatsDSender.class),
        "test"
    );

    tpe.execute(TASK);
    tpe.execute(TASK);
    tpe.execute(TASK);
    tpe.execute(TASK);

    boolean rejected = false;

    try {
      IntStream.range(0, 5).forEach(i -> tpe.execute(TASK));
      fail("RejectedExecutionException not thrown");
    } catch (RejectedExecutionException e) {
      rejected = true;
    }

    assertTrue(rejected);
    LATCH.countDown();
  }

  @BeforeEach
  public void setUp() {
    memoryAppender.start();
    Logger logger = (Logger) LoggerFactory.getLogger(MonitoredThreadPoolExecutor.class);
    logger.addAppender(memoryAppender);
  }

  @AfterEach
  public void tearDown() {
    Logger logger = (Logger) LoggerFactory.getLogger(MonitoredThreadPoolExecutor.class);
    logger.detachAppender(memoryAppender);
    memoryAppender.stop();
    memoryAppender.list.clear();
  }

  @Test
  public void testLongTaskLogging() throws InterruptedException, ExecutionException {
    long sleepMs = 100L;
    var properties = new Properties();
    properties.setProperty("minSize", "1");
    properties.setProperty("maxSize", "1");
    properties.setProperty("longTaskDurationMs", String.valueOf(sleepMs));

    var tpe = MonitoredThreadPoolExecutor.create(
        properties,
        "test",
        mock(StatsDSender.class),
        "test"
    );
    var f = executeSleepTaskOnExecutor(sleepMs, tpe);
    f.get();
    // 1 thread executor will start second task iff first task is fully done including afterExecute
    f = executeSleepTaskOnExecutor(1, tpe);
    f.get();
    assertTrue(memoryAppender.list.stream().anyMatch(event -> event.getMessage().contains("thread pool task execution took too long")));
  }

  @Test
  public void testShortTaskLogging() throws InterruptedException, ExecutionException {
    long sleepMs = 100L;
    var properties = new Properties();
    properties.setProperty("minSize", "1");
    properties.setProperty("maxSize", "1");
    properties.setProperty("longTaskDurationMs", String.valueOf(sleepMs));

    var tpe = MonitoredThreadPoolExecutor.create(
        properties,
        "test",
        mock(StatsDSender.class),
        "test"
    );
    var f = executeSleepTaskOnExecutor(1, tpe);
    f.get();
    // 1 thread executor will start second task iff first task is fully done including afterExecute
    f = executeSleepTaskOnExecutor(1, tpe);
    f.get();
    assertFalse(memoryAppender.list.stream().anyMatch(event -> event.getMessage().contains("thread pool task execution took too long")));
  }

  private static CompletableFuture<Object> executeSleepTaskOnExecutor(long sleepMs, ThreadPoolExecutor tpe) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(sleepMs);
      } catch (InterruptedException e) {
        //
      }
      return null;
    }, tpe);
  }

  private static final CountDownLatch LATCH = new CountDownLatch(1);
  private static final Runnable TASK = () -> {
    try {
      LATCH.await();
    } catch (InterruptedException e) {
      //
    }
  };
}
