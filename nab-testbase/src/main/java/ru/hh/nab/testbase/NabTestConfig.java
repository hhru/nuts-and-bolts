package ru.hh.nab.testbase;

import ru.hh.consul.AclClient;
import ru.hh.consul.AgentClient;
import ru.hh.consul.CatalogClient;
import ru.hh.consul.Consul;
import ru.hh.consul.CoordinateClient;
import ru.hh.consul.EventClient;
import ru.hh.consul.HealthClient;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.OperatorClient;
import ru.hh.consul.PreparedQueryClient;
import ru.hh.consul.SessionClient;
import ru.hh.consul.SnapshotClient;
import ru.hh.consul.StatusClient;
import ru.hh.consul.config.ClientConfig;
import ru.hh.consul.monitoring.ClientEventCallback;
import ru.hh.consul.monitoring.ClientEventHandler;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.eclipse.jetty.util.thread.ThreadPool;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.starter.NabCommonConfig;
import static ru.hh.nab.starter.server.jetty.JettyServerFactory.createJettyThreadPool;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.JETTY;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
@Import({NabCommonConfig.class})
public class NabTestConfig {
  public static final String TEST_SERVICE_NAME = "testService";
  static final String TEST_PROPERTIES_FILE_NAME = "service-test.properties";

  @Bean
  Properties serviceProperties() throws IOException {
    return createProperties(TEST_PROPERTIES_FILE_NAME);
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ThreadPool jettyThreadPool(FileSettings fileSettings, String serviceName, StatsDSender statsDSender) throws Exception {
    return createJettyThreadPool(fileSettings.getSubSettings(JETTY), serviceName, statsDSender);
  }

  @Bean
  Consul consul() {

    ClientConfig config = new ClientConfig();
    Consul.NetworkTimeoutConfig networkTimeoutConfig = new Consul.NetworkTimeoutConfig.Builder().withReadTimeout(11_000).build();
    HealthClient healthMock = mock(HealthClient.class);
    when(healthMock.getConfig()).thenReturn(config);
    when(healthMock.getNetworkTimeoutConfig()).thenReturn(networkTimeoutConfig);
    when(healthMock.getEventHandler()).thenReturn(new ClientEventHandler("health", new ClientEventCallback() {
    }));
    KeyValueClient kvMock = mock(KeyValueClient.class);
    when(kvMock.getConfig()).thenReturn(config);
    when(kvMock.getNetworkTimeoutConfig()).thenReturn(networkTimeoutConfig);
    when(kvMock.getEventHandler()).thenReturn(new ClientEventHandler("keyvalue", new ClientEventCallback() {
    }));
    return new Consul(mock(AgentClient.class), healthMock, kvMock, mock(CatalogClient.class), mock(StatusClient.class),
      mock(SessionClient.class), mock(EventClient.class), mock(PreparedQueryClient.class), mock(CoordinateClient.class),
      mock(OperatorClient.class), Executors.newSingleThreadExecutor(),
      new ConnectionPool(1, 2, TimeUnit.MINUTES),
      mock(AclClient.class), mock(SnapshotClient.class), mock(OkHttpClient.class)
    ) {};
  }

  @Bean
  StatsDClient statsDClient() {
    return new NoOpStatsDClient();
  }

  public static Properties createProperties(String propertiesName) throws IOException {
    PropertiesFactoryBean properties = new PropertiesFactoryBean();
    properties.setSingleton(false);
    properties.setIgnoreResourceNotFound(true);
    properties.setLocations(new ClassPathResource(propertiesName));
    return properties.getObject();
  }
}
