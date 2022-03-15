package ru.hh.nab.neo.starter;

import io.sentry.spring.boot.SentryAutoConfiguration;
import java.lang.management.ManagementFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.filter.RequestContextFilter;
import ru.hh.nab.neo.starter.filters.RequestIdLoggingFilter;
import ru.hh.nab.neo.starter.props.NabProperties;
import ru.hh.nab.neo.starter.server.NabJettyWebServerFactoryCustomizer;

@SpringBootConfiguration
//@EnableAutoConfiguration
@EnableConfigurationProperties({
    NabProperties.class,
})
@Import({
    ServletWebServerFactoryAutoConfiguration.class,
    EmbeddedWebServerFactoryCustomizerAutoConfiguration.class,
    NabProdConfig.class,
    NabJettyWebServerFactoryCustomizer.class,
    SentryAutoConfiguration.class //TODO: не проверял
})
public class TestServerConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestServerConfiguration.class);

  private static String getCurrentPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  public static void main(String[] args) {
    SpringApplication.run(TestServerConfiguration.class, args);
  }

  @Bean
  public FilterRegistrationBean<RequestContextFilter> requestContextFilter() {
    FilterRegistrationBean<RequestContextFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new RequestContextFilter());
    return registrationBean;
  }

//TODO: закомментировал из-за того что файл из nab-logging, а там fileSettings
//  @Bean
//  @ConditionalOnBean(LogLevelOverrideExtension.class)
//  public LogLevelOverrideApplier logLevelOverrideApplier(LogLevelOverrideExtension extension, NabProperties nabProperties) {
//    LogLevelOverrideApplier applier = new LogLevelOverrideApplier();
//    return applier.run(extension, nabProperties);
//  }

  @Bean
  public FilterRegistrationBean<RequestIdLoggingFilter> requestLoggingFilter() {
    FilterRegistrationBean<RequestIdLoggingFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new RequestIdLoggingFilter());
    return registrationBean;
  }

  @Bean
  public ApplicationListener<ApplicationReadyEvent> logStartupInfo(AppMetadata appMetadata) {
    return event -> LOGGER.info("Started {} PID={} (version={})", appMetadata.getServiceName(), getCurrentPid(), appMetadata.getVersion());
  }
}
