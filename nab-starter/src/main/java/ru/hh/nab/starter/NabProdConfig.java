package ru.hh.nab.starter;

import com.ecwid.consul.v1.ConsulClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

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
  ConsulClient consulClient(FileSettings fileSettings) {
    return new ConsulClient("localhost", fileSettings.getInteger("consul.http.port"));
  }

  @Bean
  @Lazy(value = false)
  ConsulService consulService(FileSettings fileSettings, String datacenter, AppMetadata appMetadata) throws UnknownHostException {
    var address = InetAddress.getLocalHost().getHostAddress();
    return new ConsulService(fileSettings, datacenter, address, appMetadata);
  }

  @Bean
  JettyEventListener customSpringEventListener(ConsulService consulService){
    return new JettyEventListener(consulService);
  }
}
