package ru.hh.nab.web.starter.discovery;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.glassfish.jersey.server.ResourceConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import org.mockito.stubbing.Answer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.hh.consul.AgentClient;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.config.ClientConfig;
import ru.hh.consul.monitoring.ClientEventCallback;
import ru.hh.consul.monitoring.ClientEventHandler;
import ru.hh.nab.consul.ConsulService;
import ru.hh.nab.consul.ConsulServiceException;
import ru.hh.nab.web.starter.configuration.NabConsulConfiguration;

public class ServiceDiscoveryTest {

  private static final String TEST_SERVICE_NAME = "test-service";
  private static final String TEST_SERVICE_VERSION = "test-version";
  private static final String TEST_NODE_NAME = "testNode";

  @Test
  public void testServiceIsUpOnConsulRegistrationAndDeregistration() {
    ConsulResponsesHolder consulResponsesHolder = new ConsulResponsesHolder();
    ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
        .listeners(consulResponsesHolder)
        .run();
    SpringApplication.exit(context);

    List<Response> consulResponses = consulResponsesHolder.getConsulResponses();
    assertEquals(2, consulResponses.size());
    assertEquals(Response.Status.OK.getStatusCode(), consulResponses.get(0).getStatus());
    assertEquals(Response.Status.OK.getStatusCode(), consulResponses.get(1).getStatus());
  }

  @Test
  public void testFailWithoutConsul() {
    ConsulServiceException exception = assertThrows(
        ConsulServiceException.class,
        () -> SpringApplication.run(TestConfiguration.class)
    );
    assertEquals("Consul is unavailable", exception.getCause().getMessage());
  }

  @Configuration
  @ImportAutoConfiguration({
      ServletWebServerFactoryAutoConfiguration.class,
      JerseyAutoConfiguration.class,
  })
  @Import(NabConsulConfiguration.class)
  public static class TestConfiguration {

    @Bean
    public ResourceConfig resourceConfig() {
      ResourceConfig resourceConfig = new ResourceConfig();
      resourceConfig.register(StatusResource.class);
      return resourceConfig;
    }

    @Bean
    ConsulService consulService(AgentClient agentClient, KeyValueClient keyValueClient) {
      return spy(new ConsulService(
          agentClient,
          keyValueClient,
          TEST_SERVICE_NAME,
          TEST_SERVICE_VERSION,
          TEST_NODE_NAME,
          Integer.MIN_VALUE,
          new Properties(),
          Set.of()
      ));
    }

    @Bean
    AgentClient agentClient() {
      AgentClient agentClient = mock(AgentClient.class);
      doThrow(new IllegalStateException("Consul is unavailable")).when(agentClient).register(any(), any());
      return agentClient;
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

  private static class ConsulResponsesHolder implements ApplicationListener<ContextRefreshedEvent> {

    private final List<Response> consulResponses = new ArrayList<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
      WebServerApplicationContext context = (WebServerApplicationContext) event.getApplicationContext();
      ConsulService consulService = context.getBean(ConsulService.class);
      Answer<Void> statusEndpointRequest = invocation -> {
        Invocation.Builder statusReq = ClientBuilder
            .newBuilder()
            .build()
            .target(UriBuilder.fromUri("http://localhost").port(context.getWebServer().getPort()).build())
            .path("status")
            .request();
        consulResponses.add(statusReq.get());
        return null;
      };
      doAnswer(statusEndpointRequest).when(consulService).register();
      doAnswer(statusEndpointRequest).when(consulService).deregister();
    }

    public List<Response> getConsulResponses() {
      return consulResponses;
    }
  }

  @Path("/status")
  public static class StatusResource {

    @GET
    public String status() {
      return "ok";
    }
  }
}
