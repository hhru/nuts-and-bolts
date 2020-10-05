package ru.hh.nab.starter;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import java.util.Optional;

import static java.util.Objects.requireNonNullElse;
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
  AgentClient consulClient(FileSettings fileSettings) {
    HostAndPort hostAndPort = HostAndPort.fromParts(
            requireNonNullElse(fileSettings.getString("consul.http.host"), "127.0.0.1"),
            fileSettings.getInteger("consul.http.port"));
    return Consul.builder().withHostAndPort(hostAndPort).build().agentClient();
  }

  @Bean
  @Lazy(value = false)
  ConsulService consulService(FileSettings fileSettings, String datacenter, AppMetadata appMetadata, AgentClient agentClient,
                              Optional<LogLevelOverrideExtension> logLevelOverrideExtensionOptional) throws UnknownHostException {
    return new ConsulService(agentClient, fileSettings, datacenter, InetAddress.getLocalHost().getHostName(), appMetadata,
            logLevelOverrideExtensionOptional.orElse(null));
  }

  @Bean
  JettyEventListener jettyEventListener(ConsulService consulService){
    return new JettyEventListener(consulService);
  }
}
