package ru.hh.nab.web.starter.discovery;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.util.HashSet;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.info.BuildProperties;
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
import ru.hh.consul.monitoring.ClientEventCallback;
import ru.hh.consul.monitoring.ClientEventHandler;
import ru.hh.consul.util.Address;
import ru.hh.nab.common.spring.boot.env.EnvironmentUtils;
import ru.hh.nab.consul.ConsulService;
import static ru.hh.nab.consul.ConsulService.CONSUL_PROPERTIES_PREFIX;
import ru.hh.nab.testbase.NabTestConfig;
import static ru.hh.nab.web.starter.configuration.NabConsulConfiguration.CONSUL_HTTP_HOST_PROPERTY;
import static ru.hh.nab.web.starter.configuration.NabConsulConfiguration.CONSUL_HTTP_PORT_PROPERTY;
import static ru.hh.nab.web.starter.configuration.NabConsulConfiguration.CONSUL_TAGS_PROPERTY;
import ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties;

public class ServiceDiscoveryTest {

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
          EnvironmentUtils.getPropertiesStartWith(environment, CONSUL_PROPERTIES_PREFIX),
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
