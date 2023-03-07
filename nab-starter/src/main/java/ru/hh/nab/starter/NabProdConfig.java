package ru.hh.nab.starter;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import java.io.IOException;
import static java.util.Objects.requireNonNullElse;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.Properties;
import javax.inject.Named;
import org.eclipse.jetty.servlet.FilterHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import ru.hh.consul.AgentClient;
import ru.hh.consul.Consul;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.util.Address;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.properties.PropertiesUtils.fromFilesInSettingsDir;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.starter.consul.ConsulMetricsTracker;
import ru.hh.nab.starter.consul.ConsulService;
import ru.hh.nab.starter.events.JettyEventListener;
import ru.hh.nab.starter.logging.LogLevelOverrideExtension;
import ru.hh.nab.starter.qualifier.Service;
import static ru.hh.nab.starter.server.cache.HttpCacheFilterFactory.createCacheFilterHolder;

@Configuration
@Import({NabCommonConfig.class})
public class NabProdConfig {

  public static final String CONSUL_PORT_PROPERTY = "consul.http.port";
  public static final String CONSUL_HOST_PROPERTY = "consul.http.host";
  public static final String CONSUL_CLIENT_CONNECT_TIMEOUT_PROPERTY = "consul.client.connectTimeoutMillis";
  public static final String CONSUL_CLIENT_READ_TIMEOUT_PROPERTY = "consul.client.readTimeoutMillis";
  public static final String CONSUL_CLIENT_WRITE_TIMEOUT_PROPERTY = "consul.client.writeTimeoutMillis";
  public static final String CONSUL_CLIENT_ACL_TOKEN = "consul.client.aclToken";

  public static final int CONSUL_DEFAULT_READ_TIMEOUT_MILLIS = 10_500;
  static final String PROPERTIES_FILE_NAME = "service.properties";

  @Bean
  @Service
  Properties serviceProperties() throws IOException {
    return fromFilesInSettingsDir(PROPERTIES_FILE_NAME);
  }

  @Bean
  StatsDClient statsDClient() {
    return new NonBlockingStatsDClientBuilder().hostname("localhost").queueSize(10_000).port(8125).build();
  }

  @Bean
  FilterHolder cacheFilter(FileSettings fileSettings, String serviceName, StatsDSender statsDSender) {
    return createCacheFilterHolder(fileSettings, serviceName, statsDSender);
  }

  @Bean
  Consul consul(FileSettings fileSettings, @Named(SERVICE_NAME) String serviceName, StatsDSender statsDSender) {
    if (isConsulDisabled(fileSettings)) {
      return null;
    }

    int port = ofNullable(fileSettings.getString(CONSUL_PORT_PROPERTY))
      .or(() -> ofNullable(System.getProperty(CONSUL_PORT_PROPERTY)))
      .map(Integer::parseInt)
      .orElseThrow(() -> new IllegalStateException(CONSUL_PORT_PROPERTY + " setting or property be provided"));
    Address address = new Address(requireNonNullElse(fileSettings.getString(CONSUL_HOST_PROPERTY), "127.0.0.1"), port);
    Consul.Builder builder = Consul.builder()
      .withConnectTimeoutMillis(fileSettings.getLong(CONSUL_CLIENT_CONNECT_TIMEOUT_PROPERTY, 10_500))
      .withReadTimeoutMillis(fileSettings.getLong(CONSUL_CLIENT_READ_TIMEOUT_PROPERTY, CONSUL_DEFAULT_READ_TIMEOUT_MILLIS))
      .withWriteTimeoutMillis(fileSettings.getLong(CONSUL_CLIENT_WRITE_TIMEOUT_PROPERTY, 10_500))
      .withAddress(address);
    builder.withClientEventCallback(new ConsulMetricsTracker(serviceName, statsDSender));
    return ofNullable(fileSettings.getString(CONSUL_CLIENT_ACL_TOKEN)).map(builder::withAclToken).orElse(builder).build();
  }

  @Bean
  AgentClient agentClient(Optional<Consul> consul) {
    return consul.map(Consul::agentClient).orElse(null);
  }

  @Bean
  KeyValueClient keyValueClient(Optional<Consul> consul) {
    return consul.map(Consul::keyValueClient).orElse(null);
  }

  @Bean
  @Lazy(value = false)
  ConsulService consulService(FileSettings fileSettings,
                              AppMetadata appMetadata,
                              Optional<AgentClient> agentClient,
                              Optional<KeyValueClient> keyValueClient,
                              @Named(NODE_NAME) String nodeName,
                              Optional<LogLevelOverrideExtension> logLevelOverrideExtensionOptional) {
    if (isConsulDisabled(fileSettings)) {
      return null;
    }
    return new ConsulService(
        agentClient.orElseThrow(),
        keyValueClient.orElseThrow(),
        fileSettings,
        appMetadata,
        nodeName,
        logLevelOverrideExtensionOptional.orElse(null));
  }

  private boolean isConsulDisabled(FileSettings fileSettings) {
    return !fileSettings.getBoolean(ConsulService.CONSUL_ENABLED_PROPERTY, true);
  }

  @Bean
  JettyEventListener jettyEventListener(Optional<ConsulService> consulService){
    return new JettyEventListener(consulService.orElse(null));
  }
}
