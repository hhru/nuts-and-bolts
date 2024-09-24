package ru.hh.nab.web;

import com.timgroup.statsd.StatsDClient;
import jakarta.ws.rs.Path;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import static org.assertj.core.api.Assertions.assertThat;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.consul.AgentClient;
import ru.hh.consul.Consul;
import ru.hh.consul.HealthClient;
import ru.hh.consul.KeyValueClient;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTER;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.metrics.StatsDSender;
import static ru.hh.nab.profile.Profiles.MAIN;
import ru.hh.nab.starter.AppMetadata;
import ru.hh.nab.starter.consul.ConsulFetcher;
import ru.hh.nab.starter.consul.ConsulService;
import ru.hh.nab.starter.server.jetty.JettySettingsConstants;
import ru.hh.nab.starter.server.jetty.MonitoredQueuedThreadPool;

public class NabWebAutoConfigurationTest {

  private static final String PROPERTY_TEMPLATE = "%s=%s";

  private static final String TEST_SERVICE_NAME = "test-service";
  private static final String TEST_NODE_NAME = "test-host";
  private static final String TEST_DATACENTER_NAME = "test-dc";

  private final String mainProfileProperty = PROPERTY_TEMPLATE.formatted("spring.profiles.active", MAIN);

  private final String[] deployProperties = new String[]{
      PROPERTY_TEMPLATE.formatted("serviceName", TEST_SERVICE_NAME),
      PROPERTY_TEMPLATE.formatted("nodeName", TEST_NODE_NAME),
      PROPERTY_TEMPLATE.formatted("datacenter", TEST_DATACENTER_NAME),
  };

  private final String[] jettyProperties = new String[]{
      PROPERTY_TEMPLATE.formatted(JettySettingsConstants.JETTY_PORT, "9999"),
  };

  private final String[] consulProperties = new String[]{
      PROPERTY_TEMPLATE.formatted("consul.http.host", "127.0.0.1"),
      PROPERTY_TEMPLATE.formatted("consul.http.port", 13199),
      PROPERTY_TEMPLATE.formatted("consul.http.ping", false),
  };

  private final String[] httpCacheProperties = new String[]{
      PROPERTY_TEMPLATE.formatted("http.cache.sizeInMb", 1),
  };

  private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(NabWebAutoConfiguration.class));

  @Test
  public void testSpringContextContainsAllBeans() {
    applicationContextRunner
        .withPropertyValues(mainProfileProperty)
        .withPropertyValues(deployProperties)
        .withPropertyValues(jettyProperties)
        .withPropertyValues(consulProperties)
        .withPropertyValues(httpCacheProperties)
        .withBean(TestResource.class)
        .run(context -> {
          // deploy info beans
          assertThat(context).getBean(SERVICE_NAME, String.class).hasToString(TEST_SERVICE_NAME);
          assertThat(context).getBean(NODE_NAME, String.class).hasToString(TEST_NODE_NAME);
          assertThat(context).getBean(DATACENTER, String.class).hasToString(TEST_DATACENTER_NAME);
          assertThat(context).hasSingleBean(FileSettings.class);
          assertThat(context).hasSingleBean(AppMetadata.class);
          assertThat(context).getBean("projectProperties").isInstanceOf(Properties.class);

          // consul beans
          assertThat(context).hasSingleBean(Consul.class);
          assertThat(context).hasSingleBean(AgentClient.class);
          assertThat(context).hasSingleBean(KeyValueClient.class);
          assertThat(context).hasSingleBean(HealthClient.class);
          assertThat(context).hasSingleBean(ConsulService.class);
          assertThat(context).hasSingleBean(ConsulFetcher.class);

          // metrics beans
          assertThat(context).hasSingleBean(StatsDSender.class);
          assertThat(context).getBean("statsDClient").isInstanceOf(StatsDClient.class);

          // scheduling beans
          assertThat(context).hasSingleBean(ScheduledExecutorService.class);

          // web beans
          assertThat(context).hasSingleBean(MonitoredQueuedThreadPool.class);
          assertThat(context).hasSingleBean(ServiceRegistrator.class);
          assertThat(context).getBean("defaultResourceConfig").isInstanceOf(ResourceConfig.class);
          assertThat(context).hasSingleBean(ResourceConfigCustomizer.class);
          assertThat(context).getBean("statusServlet").isInstanceOf(ServletRegistrationBean.class);
          assertThat(context).getBean("requestIdLoggingFilter").isInstanceOf(FilterRegistrationBean.class);
          assertThat(context).getBean("commonHeadersFilter").isInstanceOf(FilterRegistrationBean.class);
          assertThat(context).getBean("cacheFilter").isInstanceOf(FilterRegistrationBean.class);
        });
  }

  @Test
  public void testSpringContextDoesNotContainConsulBeansWithFailedConditions() {
    applicationContextRunner
        .withPropertyValues(deployProperties)
        .withConfiguration(UserConfigurations.of(TestConfiguration.class))
        .run(context -> {
          assertThat(context).doesNotHaveBean(Consul.class);
          assertThat(context).doesNotHaveBean(AgentClient.class);
          assertThat(context).doesNotHaveBean(KeyValueClient.class);
          assertThat(context).doesNotHaveBean(HealthClient.class);
          assertThat(context).doesNotHaveBean(ConsulService.class);
          assertThat(context).doesNotHaveBean(ConsulFetcher.class);
        });
  }

  @Test
  public void testSpringContextDoesNotContainStatsDClientBeanWithFailedConditions() {
    applicationContextRunner
        .withPropertyValues(deployProperties)
        .withConfiguration(UserConfigurations.of(TestConfiguration.class))
        .run(context -> assertThat(context).doesNotHaveBean("statsDClient"));
  }

  @Test
  public void testSpringContextDoesNotContainServiceRegistrarBeanWithFailedConditions() {
    applicationContextRunner
        .withPropertyValues(deployProperties)
        .withConfiguration(UserConfigurations.of(TestConfiguration.class))
        .run(context -> assertThat(context).doesNotHaveBean(ServiceRegistrator.class));
  }

  @Test
  public void testSpringContextDoesNotContainDefaultResourceConfigBeanWithFailedConditions() {
    applicationContextRunner
        .withPropertyValues(deployProperties)
        .withConfiguration(UserConfigurations.of(TestConfiguration.class))
        .run(context -> assertThat(context).doesNotHaveBean("defaultResourceConfig"));

    applicationContextRunner
        .withPropertyValues(deployProperties)
        .withConfiguration(UserConfigurations.of(TestConfiguration.class))
        .withBean(ResourceConfig.class)
        .withBean(TestResource.class)
        .run(context -> assertThat(context).doesNotHaveBean("defaultResourceConfig"));
  }

  @Test
  public void testSpringContextDoesNotContainResourceConfigCustomizerBeanWithFailedConditions() {
    applicationContextRunner
        .withPropertyValues(deployProperties)
        .withConfiguration(UserConfigurations.of(TestConfiguration.class))
        .run(context -> assertThat(context).doesNotHaveBean(ResourceConfigCustomizer.class));
  }

  @Test
  public void testSpringContextDoesNotContainCacheFilterBeanWithFailedConditions() {
    applicationContextRunner
        .withPropertyValues(mainProfileProperty)
        .withPropertyValues(deployProperties)
        .withPropertyValues(jettyProperties)
        .withPropertyValues(consulProperties)
        .run(context -> assertThat(context).doesNotHaveBean("cacheFilter"));

    applicationContextRunner
        .withPropertyValues(deployProperties)
        .withPropertyValues(httpCacheProperties)
        .withConfiguration(UserConfigurations.of(TestConfiguration.class))
        .run(context -> assertThat(context).doesNotHaveBean("cacheFilter"));
  }

  @Configuration
  public static class TestConfiguration {

    @Bean
    public StatsDClient mockedStatsDClient() {
      return mock(StatsDClient.class);
    }
  }

  @SuppressWarnings("RestResourceMethodInspection")
  @Path("")
  private static class TestResource {
  }
}
