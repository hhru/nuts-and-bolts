package ru.hh.nab.neo.starter;

import com.timgroup.statsd.StatsDClient;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import javax.inject.Named;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.hh.nab.common.executor.ScheduledExecutor;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTER;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.neo.starter.metrics.JvmMetricsSender;
import ru.hh.nab.neo.starter.props.NabProperties;
import ru.hh.nab.neo.starter.server.MonitoredQueuedThreadPool;

@Configuration
public class NabCommonConfig {

  public static MonitoredQueuedThreadPool createJettyThreadPool(NabProperties.Server.Threads threadsProps,
                                                                String serviceName,
                                                                ServerProperties.Jetty.Threads threads,
                                                                StatsDSender statsDSender) throws Exception {
    MonitoredQueuedThreadPool threadPool = new MonitoredQueuedThreadPool(
        threads.getMax(),
        threads.getMin(),
        threadsProps.getThreadPoolIdleTimeoutMs(),
        new BlockingArrayQueue<>(Optional.ofNullable(threadsProps.getQueueSize()).orElse(threads.getMax())),
        serviceName,
        statsDSender
    );
    threadPool.start();
    return threadPool;
  }

  @Named(SERVICE_NAME)
  @Bean(SERVICE_NAME)
  String serviceName(NabProperties nabProperties) {
    return ofNullable(nabProperties.getServiceName()).filter(Predicate.not(String::isEmpty))
      .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", SERVICE_NAME)));
  }

  @Named(DATACENTER)
  @Bean(DATACENTER)
  String datacenter(NabProperties nabProperties) {
    return ofNullable(nabProperties.getDatacenter()).filter(Predicate.not(String::isEmpty))
      .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", DATACENTER)));
  }

  @Named(NODE_NAME)
  @Bean(NODE_NAME)
  String nodeName(NabProperties nabProperties) {
    return ofNullable(nabProperties.getNodeName()).filter(Predicate.not(String::isEmpty))
      .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", NODE_NAME)));
  }

  @Bean
  MonitoredQueuedThreadPool jettyThreadPool(NabProperties nabProperties,
                                            ServerProperties serverProperties,
                                            StatsDSender statsDSender) throws Exception {
    return createJettyThreadPool(
        nabProperties.getServer().getThreads(),
        nabProperties.getServiceName(),
        serverProperties.getJetty().getThreads(),
        statsDSender
    );
  }

  @Bean
  ScheduledExecutorService scheduledExecutorService() {
    return new ScheduledExecutor();
  }

  @Bean
  StatsDSender statsDSender(ScheduledExecutorService scheduledExecutorService,
                            StatsDClient statsDClient,
                            NabProperties nabProperties) {
    StatsDSender statsDSender = new StatsDSender(statsDClient, scheduledExecutorService);
    if (Boolean.TRUE.equals(nabProperties.getMetrics().isJvmEnabled())) {
      JvmMetricsSender.create(statsDSender, nabProperties.getServiceName());
    }
    return statsDSender;
  }

  @Bean
  //TODO: можно сделать пропертей в application.properties и подключить как доп. файл пропертей
  PropertiesFactoryBean projectProperties() {
    PropertiesFactoryBean projectProps = new PropertiesFactoryBean();
    projectProps.setLocation(new ClassPathResource(AppMetadata.PROJECT_PROPERTIES));
    projectProps.setIgnoreResourceNotFound(true);
    return projectProps;
  }

  @Bean
  AppMetadata appMetadata(String serviceName, Properties projectProperties) {
    return new AppMetadata(serviceName, projectProperties);
  }
}
