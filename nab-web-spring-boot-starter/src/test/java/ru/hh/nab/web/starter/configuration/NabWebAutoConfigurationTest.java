package ru.hh.nab.web.starter.configuration;

import com.timgroup.statsd.StatsDClient;
import jakarta.ws.rs.Path;
import static java.lang.String.join;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import static org.assertj.core.api.Assertions.assertThat;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.consul.AgentClient;
import ru.hh.consul.Consul;
import ru.hh.consul.HealthClient;
import ru.hh.consul.KeyValueClient;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTER;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTERS;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_VERSION;
import static ru.hh.nab.common.spring.boot.profile.Profiles.MAIN;
import ru.hh.nab.common.spring.boot.web.servlet.SystemFilterRegistrationBean;
import ru.hh.nab.metrics.StatsDProperties;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.clients.JvmMetricsSender;
import ru.hh.nab.web.consul.ConsulFetcher;
import ru.hh.nab.web.consul.ConsulProperties;
import static ru.hh.nab.web.consul.ConsulProperties.CONSUL_ENABLED_PROPERTY;
import static ru.hh.nab.web.consul.ConsulProperties.CONSUL_HTTP_HOST_PROPERTY;
import static ru.hh.nab.web.consul.ConsulProperties.CONSUL_HTTP_PING_PROPERTY;
import static ru.hh.nab.web.consul.ConsulProperties.CONSUL_HTTP_PORT_PROPERTY;
import static ru.hh.nab.web.consul.ConsulProperties.CONSUL_REGISTRATION_ENABLED_PROPERTY;
import ru.hh.nab.web.consul.ConsulService;
import ru.hh.nab.web.consul.ConsulTagsSupplier;
import ru.hh.nab.web.jersey.filter.CacheFilter;
import ru.hh.nab.web.logging.LogLevelOverrideApplier;
import ru.hh.nab.web.logging.LogLevelOverrideExtension;
import ru.hh.nab.web.starter.configuration.properties.ExtendedServerProperties;
import ru.hh.nab.web.starter.configuration.properties.HttpCacheProperties;
import static ru.hh.nab.web.starter.configuration.properties.HttpCacheProperties.HTTP_CACHE_SIZE_PROPERTY;
import ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties;
import static ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties.DATACENTERS_PROPERTY;
import static ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties.DATACENTER_PROPERTY;
import static ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties.NODE_NAME_PROPERTY;
import static ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties.SERVICE_NAME_PROPERTY;
import ru.hh.nab.web.starter.configuration.properties.JaxbProperties;
import ru.hh.nab.web.starter.configuration.properties.LogLevelOverrideExtensionProperties;
import ru.hh.nab.web.starter.discovery.ServiceDiscoveryInitializer;
import ru.hh.nab.web.starter.jetty.MonitoredQueuedThreadPoolFactory;
import ru.hh.nab.web.starter.jetty.NabJettyServerCustomizer;
import ru.hh.nab.web.starter.jetty.NabJettyWebServerFactoryCustomizer;

public class NabWebAutoConfigurationTest {

  private static final String PROPERTY_TEMPLATE = "%s=%s";

  private static final String TEST_SERVICE_NAME = "test-service";
  private static final String TEST_SERVICE_VERSION = "test-version";
  private static final String TEST_NODE_NAME = "test-host";
  private static final String TEST_DATACENTER_NAME = "test-dc1";
  private static final List<String> TEST_DATACENTER_NAMES = List.of("test-dc1", "test-dc2");
  private static final int TEST_PORT = 0;

  private static final String STATSD_CLIENT_BEAN_NAME = "statsDClient";
  private static final String STATUS_SERVLET_BEAN_NAME = "statusServlet";
  private static final String LOG_LEVEL_OVERRIDE_CONSUL_TAG_SUPPLIER_BEAN_NAME = "logLevelOverrideConsulTagSupplier";
  private static final String REQUEST_ID_LOGGING_FILTER_BEAN_NAME = "requestIdLoggingFilter";
  private static final String COMMON_HEADERS_FILTER_BEAN_NAME = "commonHeadersFilter";
  private static final String REQUEST_CONTEXT_FILTER_BEAN_NAME = "requestContextFilter";
  private static final String DEFAULT_RESOURCE_CONFIG_BEAN_NAME = "defaultResourceConfig";

  private final String mainProfileProperty = PROPERTY_TEMPLATE.formatted("spring.profiles.active", MAIN);

  private final String[] infrastructureProperties = new String[]{
      PROPERTY_TEMPLATE.formatted(SERVICE_NAME_PROPERTY, TEST_SERVICE_NAME),
      PROPERTY_TEMPLATE.formatted(NODE_NAME_PROPERTY, TEST_NODE_NAME),
      PROPERTY_TEMPLATE.formatted(DATACENTER_PROPERTY, TEST_DATACENTER_NAME),
      PROPERTY_TEMPLATE.formatted(DATACENTERS_PROPERTY, join(",", TEST_DATACENTER_NAMES)),
  };

  private final String[] consulProperties = new String[]{
      PROPERTY_TEMPLATE.formatted(CONSUL_HTTP_HOST_PROPERTY, "127.0.0.1"),
      PROPERTY_TEMPLATE.formatted(CONSUL_HTTP_PORT_PROPERTY, 13199),
      PROPERTY_TEMPLATE.formatted(CONSUL_HTTP_PING_PROPERTY, false),
  };

  private final String[] httpCacheProperties = new String[]{
      PROPERTY_TEMPLATE.formatted(HTTP_CACHE_SIZE_PROPERTY, 1),
  };

  private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(NabWebAutoConfiguration.class))
      .withBean(BuildProperties.class, () -> {
        BuildProperties buildProperties = mock(BuildProperties.class);
        when(buildProperties.getVersion()).thenReturn(TEST_SERVICE_VERSION);
        return buildProperties;
      })
      .withBean(ServerProperties.class, () -> {
        ServerProperties serverProperties = new ServerProperties();
        serverProperties.setPort(TEST_PORT);
        return serverProperties;
      });

  @Test
  public void testSpringContextContainsAllBeans() {
    applicationContextRunner
        .withPropertyValues(mainProfileProperty)
        .withPropertyValues(infrastructureProperties)
        .withPropertyValues(consulProperties)
        .withPropertyValues(PROPERTY_TEMPLATE.formatted(NabMetricsConfiguration.METRICS_JVM_ENABLED_PROPERTY, true))
        .withPropertyValues(httpCacheProperties)
        .withBean(TestResource.class)
        .withBean("logLevelOverrideExtensionBean", LogLevelOverrideExtension.class, () -> mock(LogLevelOverrideExtension.class))
        .run(context -> {
          // deploy info beans
          assertThat(context).hasBean(SERVICE_NAME).getBean(SERVICE_NAME).isInstanceOf(String.class).hasToString(TEST_SERVICE_NAME);
          assertThat(context).hasBean(SERVICE_VERSION).getBean(SERVICE_VERSION).isInstanceOf(String.class).hasToString(TEST_SERVICE_VERSION);
          assertThat(context).hasBean(NODE_NAME).getBean(NODE_NAME).isInstanceOf(String.class).hasToString(TEST_NODE_NAME);
          assertThat(context).hasBean(DATACENTER).getBean(DATACENTER).isInstanceOf(String.class).hasToString(TEST_DATACENTER_NAME);
          assertThat(context).hasBean(DATACENTERS).getBean(DATACENTERS).isInstanceOf(List.class).isEqualTo(TEST_DATACENTER_NAMES);
          assertThat(context).hasSingleBean(FileSettings.class);
          assertThat(context).hasSingleBean(InfrastructureProperties.class);

          // consul beans
          assertThat(context).hasSingleBean(Consul.class);
          assertThat(context).hasSingleBean(AgentClient.class);
          assertThat(context).hasSingleBean(KeyValueClient.class);
          assertThat(context).hasSingleBean(HealthClient.class);
          assertThat(context).hasSingleBean(ConsulService.class);
          assertThat(context).hasSingleBean(ServiceDiscoveryInitializer.class);
          assertThat(context).hasSingleBean(ConsulFetcher.class);
          assertThat(context).hasSingleBean(ConsulProperties.class);

          // metrics beans
          assertThat(context).hasSingleBean(StatsDSender.class);
          assertThat(context).hasSingleBean(JvmMetricsSender.class);
          assertThat(context).hasBean(STATSD_CLIENT_BEAN_NAME).getBean(STATSD_CLIENT_BEAN_NAME).isInstanceOf(StatsDClient.class);
          assertThat(context).hasSingleBean(StatsDProperties.class);

          // scheduling beans
          assertThat(context).hasSingleBean(ScheduledExecutorService.class);

          // logging beans
          assertThat(context).hasSingleBean(LogLevelOverrideApplier.class);
          assertThat(context)
              .hasBean(LOG_LEVEL_OVERRIDE_CONSUL_TAG_SUPPLIER_BEAN_NAME)
              .getBean(LOG_LEVEL_OVERRIDE_CONSUL_TAG_SUPPLIER_BEAN_NAME)
              .isInstanceOf(ConsulTagsSupplier.class);
          assertThat(context).hasSingleBean(LogLevelOverrideExtensionProperties.class);

          // web beans
          assertThat(context).hasSingleBean(NabJettyWebServerFactoryCustomizer.class);
          assertThat(context).hasSingleBean(NabJettyServerCustomizer.class);
          assertThat(context).hasSingleBean(MonitoredQueuedThreadPoolFactory.class);
          assertThat(context)
              .hasBean(DEFAULT_RESOURCE_CONFIG_BEAN_NAME)
              .getBean(DEFAULT_RESOURCE_CONFIG_BEAN_NAME)
              .isInstanceOf(ResourceConfig.class);
          assertThat(context).hasSingleBean(ResourceConfigCustomizer.class);
          assertThat(context).hasBean(STATUS_SERVLET_BEAN_NAME).getBean(STATUS_SERVLET_BEAN_NAME).isInstanceOf(ServletRegistrationBean.class);
          assertThat(context)
              .hasBean(REQUEST_ID_LOGGING_FILTER_BEAN_NAME)
              .getBean(REQUEST_ID_LOGGING_FILTER_BEAN_NAME)
              .isInstanceOf(SystemFilterRegistrationBean.class);
          assertThat(context)
              .hasBean(COMMON_HEADERS_FILTER_BEAN_NAME)
              .getBean(COMMON_HEADERS_FILTER_BEAN_NAME)
              .isInstanceOf(SystemFilterRegistrationBean.class);
          assertThat(context)
              .hasBean(REQUEST_CONTEXT_FILTER_BEAN_NAME)
              .getBean(REQUEST_CONTEXT_FILTER_BEAN_NAME)
              .isInstanceOf(SystemFilterRegistrationBean.class);
          assertThat(context).hasSingleBean(CacheFilter.class);
          assertThat(context).hasSingleBean(ExtendedServerProperties.class);
          assertThat(context).hasSingleBean(HttpCacheProperties.class);
          assertThat(context).hasSingleBean(JaxbProperties.class);
        });
  }

  @Test
  public void testSpringContextDoesNotContainConsulBeansWithFailedConditions() {
    // without main profile
    applicationContextRunner
        .withPropertyValues(infrastructureProperties)
        .withPropertyValues(consulProperties)
        .withUserConfiguration(TestConfiguration.class)
        .run(context -> {
          assertThat(context).doesNotHaveBean(Consul.class);
          assertThat(context).doesNotHaveBean(AgentClient.class);
          assertThat(context).doesNotHaveBean(KeyValueClient.class);
          assertThat(context).doesNotHaveBean(HealthClient.class);
          assertThat(context).doesNotHaveBean(ConsulService.class);
          assertThat(context).doesNotHaveBean(ServiceDiscoveryInitializer.class);
          assertThat(context).doesNotHaveBean(ConsulFetcher.class);
          assertThat(context).doesNotHaveBean(ConsulProperties.class);
        });

    // when consul.enabled=false
    applicationContextRunner
        .withPropertyValues(mainProfileProperty)
        .withPropertyValues(infrastructureProperties)
        .withPropertyValues(consulProperties)
        .withPropertyValues(PROPERTY_TEMPLATE.formatted(CONSUL_ENABLED_PROPERTY, false))
        .run(context -> {
          assertThat(context).doesNotHaveBean(Consul.class);
          assertThat(context).doesNotHaveBean(AgentClient.class);
          assertThat(context).doesNotHaveBean(KeyValueClient.class);
          assertThat(context).doesNotHaveBean(HealthClient.class);
          assertThat(context).doesNotHaveBean(ConsulService.class);
          assertThat(context).doesNotHaveBean(ServiceDiscoveryInitializer.class);
          assertThat(context).doesNotHaveBean(ConsulFetcher.class);
          assertThat(context).doesNotHaveBean(ConsulProperties.class);
        });

    // when consul.registration.enabled=false
    applicationContextRunner
        .withPropertyValues(mainProfileProperty)
        .withPropertyValues(infrastructureProperties)
        .withPropertyValues(consulProperties)
        .withPropertyValues(PROPERTY_TEMPLATE.formatted(CONSUL_REGISTRATION_ENABLED_PROPERTY, false))
        .run(context -> {
          assertThat(context).doesNotHaveBean(ConsulService.class);
          assertThat(context).doesNotHaveBean(ServiceDiscoveryInitializer.class);
        });
  }

  @Test
  public void testSpringContextDoesNotContainStatsDClientBeanWithFailedConditions() {
    // without main profile
    applicationContextRunner
        .withPropertyValues(infrastructureProperties)
        .withPropertyValues(consulProperties)
        .withUserConfiguration(TestConfiguration.class)
        .run(context -> assertThat(context).doesNotHaveBean(STATSD_CLIENT_BEAN_NAME));
  }

  @Test
  public void testSpringContextDoesNotContainJvmMetricsSenderBeanWithFailedConditions() {
    // when metrics.jvm.enabled=false
    applicationContextRunner
        .withPropertyValues(infrastructureProperties)
        .withPropertyValues(PROPERTY_TEMPLATE.formatted(NabMetricsConfiguration.METRICS_JVM_ENABLED_PROPERTY, false))
        .withUserConfiguration(TestConfiguration.class)
        .run(context -> assertThat(context).doesNotHaveBean(JvmMetricsSender.class));

    // when metrics.jvm.enabled property doesn't exist
    applicationContextRunner
        .withPropertyValues(infrastructureProperties)
        .withUserConfiguration(TestConfiguration.class)
        .run(context -> assertThat(context).doesNotHaveBean(JvmMetricsSender.class));
  }

  @Test
  public void testSpringContextDoesNotContainLoggingBeansWithFailedConditions() {
    // when LogLevelOverrideExtension bean doesn't exist
    applicationContextRunner
        .withPropertyValues(infrastructureProperties)
        .withUserConfiguration(TestConfiguration.class)
        .run(context -> {
          assertThat(context).doesNotHaveBean(LogLevelOverrideExtensionProperties.class);
          assertThat(context).doesNotHaveBean(LogLevelOverrideApplier.class);
          assertThat(context).doesNotHaveBean(LOG_LEVEL_OVERRIDE_CONSUL_TAG_SUPPLIER_BEAN_NAME);
        });
  }

  @Test
  public void testSpringContextDoesNotContainDefaultResourceConfigBeanWithFailedConditions() {
    // when @Path beans don't exist
    applicationContextRunner
        .withPropertyValues(infrastructureProperties)
        .withUserConfiguration(TestConfiguration.class)
        .run(context -> assertThat(context).doesNotHaveBean(DEFAULT_RESOURCE_CONFIG_BEAN_NAME));

    // when another ResourceConfig bean exists
    applicationContextRunner
        .withPropertyValues(infrastructureProperties)
        .withUserConfiguration(TestConfiguration.class)
        .withBean(ResourceConfig.class)
        .withBean(TestResource.class)
        .run(context -> assertThat(context).doesNotHaveBean(DEFAULT_RESOURCE_CONFIG_BEAN_NAME));
  }

  @Test
  public void testSpringContextDoesNotContainResourceConfigCustomizerBeanWithFailedConditions() {
    // when ResourceConfig bean doesn't exist
    applicationContextRunner
        .withPropertyValues(infrastructureProperties)
        .withUserConfiguration(TestConfiguration.class)
        .run(context -> assertThat(context).doesNotHaveBean(ResourceConfigCustomizer.class));
  }

  @Test
  public void testSpringContextDoesNotContainCacheFilterBeanWithFailedConditions() {
    // when http.cache.sizeInMb property doesn't exist
    applicationContextRunner
        .withPropertyValues(mainProfileProperty)
        .withPropertyValues(infrastructureProperties)
        .withPropertyValues(consulProperties)
        .run(context -> assertThat(context).doesNotHaveBean(CacheFilter.class));

    // without main profile
    applicationContextRunner
        .withPropertyValues(infrastructureProperties)
        .withPropertyValues(consulProperties)
        .withPropertyValues(httpCacheProperties)
        .withUserConfiguration(TestConfiguration.class)
        .run(context -> assertThat(context).doesNotHaveBean(CacheFilter.class));
  }

  @Test
  public void testInfrastructurePropertiesValidation() {
    applicationContextRunner
        .run(context -> {
          assertThat(context)
              .hasFailed()
              .getFailure()
              .rootCause()
              .isInstanceOf(BindValidationException.class)
              .hasMessageContaining(SERVICE_NAME_PROPERTY)
              .hasMessageContaining(NODE_NAME_PROPERTY)
              .hasMessageContaining(DATACENTER_PROPERTY)
              .hasMessageContaining(DATACENTERS_PROPERTY);
        });
  }

  @Test
  public void testConsulPropertiesValidation() {
    applicationContextRunner
        .withPropertyValues(mainProfileProperty)
        .withPropertyValues(infrastructureProperties)
        .run(context -> {
          assertThat(context)
              .hasFailed()
              .getFailure()
              .rootCause()
              .isInstanceOf(BindValidationException.class)
              .hasMessageContaining(CONSUL_HTTP_PORT_PROPERTY);
        });
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
