package ru.hh.nab.web;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.web.resource.StatusResource;

@Configuration
@ImportAutoConfiguration({
    ServletWebServerFactoryAutoConfiguration.class,
    JerseyAutoConfiguration.class
})
public class NabWebTestConfig {

  public static final String TEST_SERVICE_NAME = "testService";
  public static final String TEST_SERVICE_VERSION = "test-version";

  @Bean
  public ServletRegistrationBean<ServletContainer> statusServlet() {
    Instant started = Instant.now().minus(5L, ChronoUnit.SECONDS);
    StatusResource statusResource = new StatusResource(
        TEST_SERVICE_NAME,
        TEST_SERVICE_VERSION,
        () -> Duration.between(started, Instant.now())
    );
    return new ServletRegistrationBean<>(
        new ServletContainer(new ResourceConfig().register(statusResource)),
        "/status"
    );
  }
}
