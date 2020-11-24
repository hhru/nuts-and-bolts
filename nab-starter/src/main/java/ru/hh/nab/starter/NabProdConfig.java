package ru.hh.nab.starter;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import java.util.Optional;

import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.eclipse.jetty.servlet.FilterHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.metrics.StatsDSender;

import static ru.hh.nab.common.properties.PropertiesUtils.fromFilesInSettingsDir;
import ru.hh.nab.starter.events.JettyEventListener;
import ru.hh.nab.starter.logging.LogLevelOverrideExtension;

import static ru.hh.nab.starter.server.cache.HttpCacheFilterFactory.createCacheFilterHolder;

@Configuration
@Import({NabCommonConfig.class})
public class NabProdConfig {

  public static final String CONSUL_PORT_PROPERTY = "consul.http.port";
  public static final String CONSUL_HOST_PROPERTY = "consul.http.host";
  public static final String CONSUL_PORT_ENV_KEY = "CONSUL_PORT";
  public static final String CONSUL_CLIENT_CONNECT_TIMEOUT_PROPERTY = "consul.client.connectTimeoutMillis";
  public static final String CONSUL_CLIENT_READ_TIMEOUT_PROPERTY = "consul.client.readTimeoutMillis";
  public static final String CONSUL_CLIENT_WRITE_TIMEOUT_PROPERTY = "consul.client.writeTimeoutMillis";
  static final String PROPERTIES_FILE_NAME = "service.properties";
  static final String DATACENTER_NAME_PROPERTY = "datacenter";

  @Bean
  Properties serviceProperties() throws IOException {
    return fromFilesInSettingsDir(PROPERTIES_FILE_NAME);
  }

  @Bean
  String datacenter(FileSettings fileSettings) {
    return ofNullable(fileSettings.getString(DATACENTER_NAME_PROPERTY))
      .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", DATACENTER_NAME_PROPERTY)));
  }

  @Bean
  StatsDClient statsDClient() {
    return new NonBlockingStatsDClient(null, "localhost", 8125, 10000);
  }

  @Bean
  FilterHolder cacheFilter(FileSettings fileSettings, String serviceName, StatsDSender statsDSender) {
    return createCacheFilterHolder(fileSettings, serviceName, statsDSender);
  }

  @Bean
  Consul consul(FileSettings fileSettings) {
    int port = ofNullable(fileSettings.getInteger(CONSUL_PORT_PROPERTY))
      .or(() -> of(System.getProperty(CONSUL_PORT_ENV_KEY)).map(Integer::valueOf))
      .orElseThrow(() -> new IllegalStateException(CONSUL_PORT_PROPERTY + " setting or " + CONSUL_PORT_ENV_KEY + " envmust be provided"));
    HostAndPort hostAndPort = HostAndPort.fromParts(requireNonNullElse(fileSettings.getString(CONSUL_HOST_PROPERTY), "127.0.0.1"), port);
    return Consul.builder()
      .withConnectTimeoutMillis(fileSettings.getLong(CONSUL_CLIENT_CONNECT_TIMEOUT_PROPERTY, 10_500))
      .withReadTimeoutMillis(fileSettings.getLong(CONSUL_CLIENT_READ_TIMEOUT_PROPERTY, 10_500))
      .withWriteTimeoutMillis(fileSettings.getLong(CONSUL_CLIENT_WRITE_TIMEOUT_PROPERTY, 10_500))
      .withHostAndPort(hostAndPort)
      .build();
  }

  @Bean
  AgentClient agentClient(Consul consul) {
    return consul.agentClient();
  }

  @Bean
  KeyValueClient keyValueClient(Consul consul) {

    return consul.keyValueClient();
  }

  @Bean
  @Lazy(value = false)
  ConsulService consulService(FileSettings fileSettings,
                              AppMetadata appMetadata,
                              AgentClient agentClient,
                              KeyValueClient keyValueClient,
                              Optional<LogLevelOverrideExtension> logLevelOverrideExtensionOptional) throws UnknownHostException {
    return new ConsulService(agentClient, keyValueClient, fileSettings, InetAddress.getLocalHost().getHostName(), appMetadata,
            logLevelOverrideExtensionOptional.orElse(null));
  }

  @Bean
  JettyEventListener jettyEventListener(ConsulService consulService){
    return new JettyEventListener(consulService);
  }
}
