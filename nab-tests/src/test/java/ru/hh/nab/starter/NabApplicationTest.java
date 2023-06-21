package ru.hh.nab.starter;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import static java.util.Objects.requireNonNullElse;
import java.util.Properties;
import org.eclipse.jetty.servlet.DefaultServlet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import static org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext;
import ru.hh.consul.AgentClient;
import ru.hh.consul.Consul;
import ru.hh.consul.util.Address;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTER;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.starter.consul.ConsulService;
import ru.hh.nab.starter.jersey.TestResource;
import ru.hh.nab.starter.qualifier.Service;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.starter.server.jetty.JettySettingsConstants;
import ru.hh.nab.testbase.NabTestConfig;

public class NabApplicationTest {

  @Test
  public void runShouldStartJetty() {
    JettyServer server = NabApplication.runWebApp(new NabServletContextConfig(), NabTestConfig.class);
    WebApplicationContext webApplicationContext = getWebApplicationContext(server.getServletContext());
    AppMetadata appMetadata = webApplicationContext.getBean(AppMetadata.class);
    long upTimeSeconds = appMetadata.getUpTimeSeconds();
    assertEquals(NabTestConfig.TEST_SERVICE_NAME, webApplicationContext.getBean("serviceName"));
    Invocation.Builder statusReq = ClientBuilder.newBuilder().build().target(UriBuilder.fromUri("http://localhost").port(server.getPort()).build())
        .path("status").request();
    try (Response response = statusReq.get()) {
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      Project project = response.readEntity(Project.class);
      assertEquals(appMetadata.getServiceName(), project.name);
      assertEquals(appMetadata.getVersion(), project.version);
      assertTrue(project.uptime >= upTimeSeconds);
    }
  }

  @Test
  public void testCloseAllContextsAfterStopJetty() {

    JettyServer server = NabApplication.runWebApp(new NabServletContextConfig(), NabTestConfig.class);
    WebApplicationContext webApplicationContext = getWebApplicationContext(server.getServletContext());
    Invocation.Builder statusReq = ClientBuilder.newBuilder().build().target(UriBuilder.fromUri("http://localhost").port(server.getPort()).build())
        .path("status").request();

    assertEquals(NabTestConfig.TEST_SERVICE_NAME, webApplicationContext.getBean("serviceName"));
    assertEquals(NabTestConfig.TEST_SERVICE_NAME, webApplicationContext.getParent().getBean("serviceName"));
    try (Response response = statusReq.get()) {
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    server.stop();

    assertFalse(server.isRunning());
    assertThrows(ProcessingException.class, statusReq::get);
    assertThrows(IllegalStateException.class, () -> webApplicationContext.getBean("serviceName"));
    assertThrows(IllegalStateException.class, () -> webApplicationContext.getParent().getBean("serviceName"));
  }


  /*@Test
  public void testServiceIsUpOnConsulRegistration() {
    AnnotationConfigWebApplicationContext aggregateCtx = new AnnotationConfigWebApplicationContext();
    aggregateCtx.register(NabAppTestConfig.class);
    aggregateCtx.refresh();

    NabApplication nabApplication = new NabApplication(new NabServletContextConfig());
    JettyServer jettyServer = nabApplication.createJettyServer(aggregateCtx, false);

    ConsulService consulService = aggregateCtx.getBean(ConsulService.class);
    doAnswer(invocation -> {
      Invocation.Builder statusReq = ClientBuilder.newBuilder().build().target(UriBuilder.fromUri("http://localhost")
              .port(jettyServer.getPort()).build())
          .path("status").request();
      try (Response response = statusReq.get()) {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      }
      return null;
    }).when(consulService).register();

    jettyServer.start();
    jettyServer.stop();
  }*/

  /*@Test
  public void testServiceIsUpOnConsulDeregistration() {
    AnnotationConfigWebApplicationContext aggregateCtx = new AnnotationConfigWebApplicationContext();
    aggregateCtx.register(NabAppTestConfig.class);
    aggregateCtx.refresh();

    NabApplication nabApplication = new NabApplication(new NabServletContextConfig());
    JettyServer jettyServer = nabApplication.createJettyServer(aggregateCtx, false);

    ConsulService consulService = aggregateCtx.getBean(ConsulService.class);
    doAnswer(invocation -> {
      Invocation.Builder statusReq = ClientBuilder.newBuilder().build().target(UriBuilder.fromUri("http://localhost")
              .port(jettyServer.getPort()).build())
          .path("status").request();
      try (Response response = statusReq.get()) {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      }
      return null;
    }).when(consulService).deregister();

    jettyServer.start();
    jettyServer.stop();
  }*/

  @Test
  public void testFailWithoutConsul() {
    AnnotationConfigWebApplicationContext aggregateCtx = new AnnotationConfigWebApplicationContext();
    aggregateCtx.register(BrokenConsulConfig.class);
    BeanCreationException exception = assertThrows(BeanCreationException.class, aggregateCtx::refresh);
    assertEquals("consulClient", exception.getBeanName());
  }

  @Test
  @ExpectSystemExitWithStatus(1)
  public void runShouldFailOnServletMappingConflict() {
    NabApplication.builder()
        .addServlet(ctx -> new DefaultServlet()).setServletName("conflictingServlet").bindTo("/status")
        .build().run(NabTestConfig.class);
  }

  @Test
  @ExpectSystemExitWithStatus(1)
  public void runShouldFailOnContextRefreshFail() {
    NabApplication.runWebApp(new NabServletContextConfig(), NabTestConfig.class, BrokenCtx.class);
  }

  @Test
  public void runShouldFailOnWrongJerseyCfg() {
    assertThrows(IllegalArgumentException.class, () ->
        NabApplication.builder().configureJersey().registerResources(TestResource.class).bindToRoot().build().run()
    );
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
  public static class BrokenConsulConfig {
    @Bean
    AgentClient consulClient(FileSettings fileSettings) {
      Address hostAndPort = new Address(
          requireNonNullElse(fileSettings.getString(NabProdConfig.CONSUL_HOST_PROPERTY), "127.0.0.1"),
          fileSettings.getInteger(NabProdConfig.CONSUL_PORT_PROPERTY)
      );
      return Consul.builder().withAddress(hostAndPort).build().agentClient();
    }

    @Bean
    @Service
    Properties serviceProperties() {
      Properties properties = new Properties();
      properties.setProperty(ConsulService.CONSUL_REGISTRATION_ENABLED_PROPERTY, "true");
      properties.setProperty(SERVICE_NAME, "testService");
      properties.setProperty(DATACENTER, "test");
      properties.setProperty(NODE_NAME, "localhost");
      properties.setProperty(NabProdConfig.CONSUL_PORT_PROPERTY, "123");
      properties.setProperty(JettySettingsConstants.JETTY_PORT, "0");
      return properties;
    }
  }
}
