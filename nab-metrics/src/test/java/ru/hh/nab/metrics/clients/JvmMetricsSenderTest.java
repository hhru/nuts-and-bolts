package ru.hh.nab.metrics.clients;

import com.timgroup.statsd.StatsDClient;
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.startsWith;
import org.mockito.Mockito;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import ru.hh.nab.metrics.StatsDSender;

public class JvmMetricsSenderTest {

  @Test
  public void testClassLoadingMetricsSent() {
    StatsDClient statsDClient = Mockito.mock(StatsDClient.class);
    ScheduledExecutorService noopExecutor = Mockito.mock(ScheduledExecutorService.class);
    StatsDSender statsDSender = Mockito.spy(new StatsDSender(statsDClient, noopExecutor));
    doNothing().when(statsDSender).sendPeriodically(Mockito.any(Runnable.class));

    JvmMetricsSender jvmMetricsSender = new JvmMetricsSender(statsDSender, "test-service");
    jvmMetricsSender.sendJvmMetrics();

    // current loaded classes and cumulative class loading counters
    verify(statsDClient, atLeastOnce()).gauge(startsWith(JvmMetricsSender.LOADED_CLASSES_COUNT_METRIC_NAME), anyLong());
    verify(statsDClient, atLeastOnce()).gauge(startsWith(JvmMetricsSender.TOTAL_LOADED_CLASSES_COUNT_METRIC_NAME), anyLong());
    verify(statsDClient, atLeastOnce()).gauge(startsWith(JvmMetricsSender.TOTAL_UNLOADED_CLASSES_COUNT_METRIC_NAME), anyLong());
  }

  @Test
  public void testCompilationMetricsSent() {
    StatsDClient statsDClient = Mockito.mock(StatsDClient.class);
    ScheduledExecutorService noopExecutor = Mockito.mock(ScheduledExecutorService.class);
    StatsDSender statsDSender = Mockito.spy(new StatsDSender(statsDClient, noopExecutor));
    doNothing().when(statsDSender).sendPeriodically(Mockito.any(Runnable.class));

    JvmMetricsSender jvmMetricsSender = new JvmMetricsSender(statsDSender, "test-service");
    jvmMetricsSender.sendJvmMetrics();

    // compilation time metric is optional and depends on JVM support
    CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
    if (compilationMXBean != null && compilationMXBean.isCompilationTimeMonitoringSupported()) {
      verify(statsDClient, atLeastOnce()).gauge(startsWith(JvmMetricsSender.TOTAL_COMPILATION_TIME_METRIC_NAME), anyLong());
    }
  }
}

