package ru.hh.nab.starter;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Map;
import static java.util.Objects.requireNonNullElse;
import org.eclipse.jetty.servlet.DefaultServlet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doAnswer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.hh.consul.AgentClient;
import ru.hh.consul.Consul;
import ru.hh.consul.util.Address;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import static ru.hh.nab.starter.consul.ConsulProperties.CONSUL_HTTP_HOST_PROPERTY;
import static ru.hh.nab.starter.consul.ConsulProperties.CONSUL_HTTP_PORT_PROPERTY;
import ru.hh.nab.starter.consul.ConsulService;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.web.InfrastructureProperties;

public class NabApplicationTest {

  @Test
  public void runShouldStartJetty() {
    ApplicationContext context = SpringApplication.run(TestConfiguration.class);
    WebServer server = ((WebServerApplicationContext) context).getWebServer();
    InfrastructureProperties infrastructureProperties = context.getBean(InfrastructureProperties.class);
    long upTimeSeconds = infrastructureProperties.getUpTime().toSeconds();
    BuildProperties buildProperties = context.getBean(BuildProperties.class);
    assertEquals(NabTestConfig.TEST_SERVICE_NAME, context.getBean(SERVICE_NAME));
    Invocation.Builder statusReq = ClientBuilder
        .newBuilder()
        .build()
        .target(UriBuilder.fromUri("http://localhost").port(server.getPort()).build())
        .path("status")
        .request();
    try (Response response = statusReq.get()) {
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      Project project = response.readEntity(Project.class);
      assertEquals(infrastructureProperties.getServiceName(), project.name);
      assertEquals(buildProperties.getVersion(), project.version);
      assertTrue(project.uptime >= upTimeSeconds);
    }
    SpringApplication.exit(context);
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

    ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
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

    ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
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

  @Test
  @ExpectSystemExitWithStatus(1)
  public void runShouldFailOnServletMappingConflict() {
    NabApplication
        .builder()
        .addServlet(ctx -> new DefaultServlet())
        .setServletName("conflictingServlet")
        .bindTo("/status")
        .build()
        .run(NabTestConfig.class);
  }

  @Test
  @ExpectSystemExitWithStatus(1)
  public void runShouldFailOnContextRefreshFail() {
    NabApplication.runWebApp(new NabServletContextConfig(), NabTestConfig.class, BrokenCtx.class);
  }

  @XmlRootElement
  private static final class Project {
    @XmlAttribute
    private String name;
    @XmlElement
    private String version;
    @XmlElement
    private long uptime;
  }

  @Configuration
  public static class BrokenCtx {
    @Bean
    String failedBean() {
      throw new RuntimeException("failed to load bean");
    }
  }

  @Configuration
  @Import(NabAppTestConfig.class)
  @EnableAutoConfiguration
  public static class BrokenConsulConfig {
    @Bean
    AgentClient consulClient(FileSettings fileSettings) {
      Address hostAndPort = new Address(
          requireNonNullElse(fileSettings.getString(CONSUL_HTTP_HOST_PROPERTY), "127.0.0.1"),
          fileSettings.getInteger(CONSUL_HTTP_PORT_PROPERTY)
      );
      return Consul.builder().withAddress(hostAndPort).build().agentClient();
    }
  }

  @Configuration
  @Import(NabAppTestConfig.class)
  @EnableAutoConfiguration
  public static class TestConfiguration {
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
