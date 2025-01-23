package ru.hh.nab.consul;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.math.BigInteger;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import ru.hh.consul.AgentClient;
import ru.hh.consul.Consul;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.config.ClientConfig;
import ru.hh.consul.model.ConsulResponse;
import ru.hh.consul.model.agent.Registration;
import ru.hh.consul.model.catalog.ServiceWeights;
import ru.hh.consul.model.kv.ImmutableValue;
import ru.hh.consul.model.kv.Value;
import ru.hh.consul.monitoring.ClientEventCallback;
import ru.hh.consul.monitoring.ClientEventHandler;
import ru.hh.consul.option.QueryOptions;
import ru.hh.consul.util.Address;
import ru.hh.nab.common.spring.boot.env.EnvironmentUtils;
import static ru.hh.nab.consul.ConsulService.CONSUL_CHECK_FAIL_COUNT_PROPERTY;
import static ru.hh.nab.consul.ConsulService.CONSUL_CHECK_HOST_PROPERTY;
import static ru.hh.nab.consul.ConsulService.CONSUL_CHECK_INTERVAL_PROPERTY;
import static ru.hh.nab.consul.ConsulService.CONSUL_CHECK_SUCCESS_COUNT_PROPERTY;
import static ru.hh.nab.consul.ConsulService.CONSUL_CHECK_TIMEOUT_PROPERTY;
import static ru.hh.nab.consul.ConsulService.CONSUL_DEREGISTER_CRITICAL_TIMEOUT_PROPERTY;
import ru.hh.nab.testbase.NabTestConfig;
import static ru.hh.nab.testbase.NabTestConfig.TEST_SERVICE_NAME;
import static ru.hh.nab.testbase.NabTestConfig.TEST_SERVICE_VERSION;
import static ru.hh.nab.web.starter.configuration.NabConsulConfiguration.CONSUL_HTTP_HOST_PROPERTY;
import static ru.hh.nab.web.starter.configuration.NabConsulConfiguration.CONSUL_HTTP_PORT_PROPERTY;
import static ru.hh.nab.web.starter.configuration.NabConsulConfiguration.CONSUL_TAGS_PROPERTY;
import ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties;
import ru.hh.nab.web.starter.discovery.ServiceDiscoveryInitializer;

public class ConsulServiceTest {

  private static final String TEST_NODE_NAME = "testNode";
  private static final String PROPERTY_TEMPLATE = "%s=%s";

  @Test
  public void testRegisterWithFullFileProperties() {
    TestPropertyValues userProperties = TestPropertyValues.of(
        PROPERTY_TEMPLATE.formatted(CONSUL_HTTP_PORT_PROPERTY, 123),
        PROPERTY_TEMPLATE.formatted(CONSUL_CHECK_HOST_PROPERTY, "localhost"),
        PROPERTY_TEMPLATE.formatted(CONSUL_CHECK_INTERVAL_PROPERTY, "33s"),
        PROPERTY_TEMPLATE.formatted(CONSUL_CHECK_TIMEOUT_PROPERTY, "42s"),
        PROPERTY_TEMPLATE.formatted(CONSUL_CHECK_SUCCESS_COUNT_PROPERTY, 7),
        PROPERTY_TEMPLATE.formatted(CONSUL_CHECK_FAIL_COUNT_PROPERTY, 8),
        PROPERTY_TEMPLATE.formatted(CONSUL_DEREGISTER_CRITICAL_TIMEOUT_PROPERTY, "13m"),
        PROPERTY_TEMPLATE.formatted(CONSUL_TAGS_PROPERTY, "tag1, tag2")
    );
    ConfigurableApplicationContext context = new SpringApplicationBuilder(CustomKVConfig.class)
        .initializers(userProperties::applyTo)
        .run();
    AgentClient agentClient = context.getBean(AgentClient.class);
    ArgumentCaptor<Registration> argument = ArgumentCaptor.forClass(Registration.class);
    verify(agentClient).register(argument.capture(), any(QueryOptions.class));
    Registration registration = argument.getValue();

    assertTrue(registration.getCheck().isPresent());
    Registration.RegCheck regCheck = registration.getCheck().get();
    assertEquals("http://localhost:0/status", regCheck.getHttp().get());
    assertEquals("33s", regCheck.getInterval().get());
    assertEquals("42s", regCheck.getTimeout().get());
    assertEquals("13m", regCheck.getDeregisterCriticalServiceAfter().get());
    assertEquals(7, regCheck.getSuccessBeforePassing().get());
    assertEquals(8, regCheck.getFailuresBeforeCritical().get());

    assertTrue(registration.getServiceWeights().isPresent());
    ServiceWeights serviceWeights = registration.getServiceWeights().get();
    assertEquals(204, serviceWeights.getPassing());
    assertEquals(0, serviceWeights.getWarning());

    assertEquals(String.join("-", TEST_SERVICE_NAME, TEST_NODE_NAME, "0"), registration.getId());
    assertEquals(TEST_SERVICE_NAME, registration.getName());
    assertEquals(0, registration.getPort().get());
    List<String> tags = registration.getTags();
    assertEquals(2, tags.size());
    assertEquals(List.of("tag1", "tag2"), tags);
    Map<String, String> meta = registration.getMeta();
    assertEquals(1, meta.size());
    assertEquals(Map.of("serviceVersion", TEST_SERVICE_VERSION), meta);
  }

  @Test
  public void testRegisterWithDefault() {
    ArgumentCaptor<Registration> defaultArgument = ArgumentCaptor.forClass(Registration.class);
    TestPropertyValues userProperties = TestPropertyValues.of(PROPERTY_TEMPLATE.formatted("server.port", "17"));
    ConfigurableApplicationContext context = new SpringApplicationBuilder(CustomKVConfig.class)
        .initializers(userProperties::applyTo)
        .run();
    AgentClient agentClient = context.getBean(AgentClient.class);
    verify(agentClient).register(defaultArgument.capture(), any(QueryOptions.class));
    Registration registration = defaultArgument.getValue();

    assertTrue(registration.getCheck().isPresent());
    Registration.RegCheck regCheck = registration.getCheck().get();
    assertEquals("http://127.0.0.1:17/status", regCheck.getHttp().get());
    assertEquals("5s", regCheck.getInterval().get());
    assertEquals("5s", regCheck.getTimeout().get());
    assertEquals("10m", regCheck.getDeregisterCriticalServiceAfter().get());
    assertEquals(1, regCheck.getSuccessBeforePassing().get());
    assertEquals(1, regCheck.getFailuresBeforeCritical().get());

    assertTrue(registration.getServiceWeights().isPresent());
    ServiceWeights serviceWeights = registration.getServiceWeights().get();
    assertEquals(204, serviceWeights.getPassing());
    assertEquals(0, serviceWeights.getWarning());


    assertEquals(String.join("-", TEST_SERVICE_NAME, TEST_NODE_NAME, "17"), registration.getId());
    assertEquals(TEST_SERVICE_NAME, registration.getName());
    assertEquals(17, registration.getPort().get());
    List<String> tags = registration.getTags();
    assertEquals(0, tags.size());
    Map<String, String> meta = registration.getMeta();
    assertEquals(1, meta.size());
    assertEquals(Map.of("serviceVersion", TEST_SERVICE_VERSION), meta);
  }

  @Test
  public void testServiceIsUpOnConsulRegistration() {
    ConsulResponseHolder consulResponseHolder = new ConsulResponseHolder();

    class ContextRefreshedEventEventListener implements ApplicationListener<ContextRefreshedEvent> {
      @Override
      public void onApplicationEvent(ContextRefreshedEvent event) {
        WebServerApplicationContext context = (WebServerApplicationContext) event.getApplicationContext();
        ConsulService consulService = context.getBean(ConsulService.class);
        doAnswer(invocation -> {
          Invocation.Builder statusReq = ClientBuilder
              .newBuilder()
              .build()
              .target(UriBuilder.fromUri("http://localhost").port(context.getWebServer().getPort()).build())
              .path("status")
              .request();
          consulResponseHolder.setResponse(statusReq.get());
          return null;
        })
            .when(consulService)
            .register();
      }
    }

    ConfigurableApplicationContext context = new SpringApplicationBuilder(BaseConsulConfig.class)
        .listeners(new ContextRefreshedEventEventListener())
        .run();

    assertEquals(Response.Status.OK.getStatusCode(), consulResponseHolder.getResponse().getStatus());

    SpringApplication.exit(context);
  }

  @Test
  public void testServiceIsUpOnConsulDeregistration() {
    ConsulResponseHolder consulResponseHolder = new ConsulResponseHolder();

    class ContextRefreshedEventEventListener implements ApplicationListener<ContextRefreshedEvent> {
      @Override
      public void onApplicationEvent(ContextRefreshedEvent event) {
        WebServerApplicationContext context = (WebServerApplicationContext) event.getApplicationContext();
        ConsulService consulService = context.getBean(ConsulService.class);
        doAnswer(invocation -> {
          Invocation.Builder statusReq = ClientBuilder
              .newBuilder()
              .build()
              .target(UriBuilder.fromUri("http://localhost").port(context.getWebServer().getPort()).build())
              .path("status")
              .request();
          consulResponseHolder.setResponse(statusReq.get());
          return null;
        })
            .when(consulService)
            .deregister();
      }
    }

    ConfigurableApplicationContext context = new SpringApplicationBuilder(BaseConsulConfig.class)
        .listeners(new ContextRefreshedEventEventListener())
        .run();
    SpringApplication.exit(context);

    assertEquals(Response.Status.OK.getStatusCode(), consulResponseHolder.getResponse().getStatus());
  }

  @Test
  public void testFailWithoutConsul() {
    BeanCreationException exception = assertThrows(
        BeanCreationException.class,
        () -> new SpringApplicationBuilder(BrokenConsulConfig.class).properties(Map.of(CONSUL_HTTP_PORT_PROPERTY, 123)).run()
    );
    assertEquals("consulClient", exception.getBeanName());
    assertTrue(exception.getMessage().contains("Error connecting to Consul"));
  }

  @Import(NabTestConfig.class)
  @EnableAutoConfiguration
  public static class BaseConsulConfig {

    @Bean
    ConsulService consulService(
        InfrastructureProperties infrastructureProperties,
        BuildProperties buildProperties,
        ServerProperties serverProperties,
        ConfigurableEnvironment environment,
        AgentClient agentClient,
        KeyValueClient keyValueClient
    ) {
      ConsulService consulService = new ConsulService(
          agentClient,
          keyValueClient,
          infrastructureProperties.getServiceName(),
          buildProperties.getVersion(),
          infrastructureProperties.getNodeName(),
          serverProperties.getPort(),
          EnvironmentUtils.getProperties(environment),
          new HashSet<>(EnvironmentUtils.getPropertyAsStringList(environment, CONSUL_TAGS_PROPERTY))
      );
      return spy(consulService);
    }

    @Bean
    AgentClient agentClient() {
      return mock(AgentClient.class);
    }

    @Bean
    KeyValueClient keyValueClient() {
      KeyValueClient mock = mock(KeyValueClient.class);
      when(mock.getConfig()).thenReturn(new ClientConfig());
      when(mock.getEventHandler()).thenReturn(new ClientEventHandler("test", new ClientEventCallback() {}));
      return mock;
    }

    @Bean
    ServiceDiscoveryInitializer serviceDiscoveryInitializer(ConsulService consulService) {
      return new ServiceDiscoveryInitializer(consulService);
    }
  }

  @Configuration
  @Import(BaseConsulConfig.class)
  public static class CustomKVConfig {
    @Bean
    KeyValueClient keyValueClient() {
      KeyValueClient mock = mock(KeyValueClient.class);
      when(mock.getConfig()).thenReturn(new ClientConfig());
      when(mock.getEventHandler()).thenReturn(new ClientEventHandler("test", new ClientEventCallback() {}));
      Value weight = ImmutableValue
          .builder()
          .createIndex(1)
          .modifyIndex(1)
          .lockIndex(1)
          .key("key")
          .flags(1)
          .value(Base64.getEncoder().encodeToString("204".getBytes()))
          .build();
      when(mock.getConsulResponseWithValue(eq(String.join("/", "host", TEST_NODE_NAME, "weight")), any(QueryOptions.class)))
          .thenReturn(Optional.of(new ConsulResponse<>(weight, 0, true, BigInteger.ONE, null, null)));
      return mock;
    }
  }

  @Configuration
  @Import(BaseConsulConfig.class)
  public static class BrokenConsulConfig {
    @Bean
    AgentClient consulClient(Environment environment) {
      Address hostAndPort = new Address(
          environment.getProperty(CONSUL_HTTP_HOST_PROPERTY, "127.0.0.1"),
          environment.getRequiredProperty(CONSUL_HTTP_PORT_PROPERTY, Integer.class)
      );
      return Consul.builder().withAddress(hostAndPort).build().agentClient();
    }
  }

  private static class ConsulResponseHolder {
    private Response response;

    public Response getResponse() {
      return response;
    }

    public void setResponse(Response response) {
      this.response = response;
    }
  }
}
