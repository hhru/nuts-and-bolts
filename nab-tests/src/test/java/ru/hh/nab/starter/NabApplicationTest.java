package ru.hh.nab.starter;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import java.util.Properties;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.eclipse.jetty.servlet.DefaultServlet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import static org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext;
import ru.hh.nab.starter.exceptions.ConsulServiceException;
import ru.hh.nab.starter.jersey.TestResource;
import ru.hh.nab.starter.server.jetty.JettyLifeCycleListener;
import ru.hh.nab.starter.server.jetty.JettyServer;
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
  public void testRightStartupOrderForConsul() {
    AnnotationConfigWebApplicationContext aggregateCtx = new AnnotationConfigWebApplicationContext();
    aggregateCtx.register(NabAppTestConfig.class);
    aggregateCtx.refresh();

    JettyLifeCycleListener lifeCycleListener = spy(new JettyLifeCycleListener(aggregateCtx));

    NabApplication nabApplication = new NabApplication(new NabServletContextConfig());
    JettyServer jettyServer = nabApplication.createJettyServer(aggregateCtx,
            false,
            portSupplier -> portSupplier.apply(null),
            webAppContext -> webAppContext.addLifeCycleListener(lifeCycleListener)
            );

    ConsulService consulService = aggregateCtx.getBean(ConsulService.class);

    jettyServer.start();

    InOrder inOrder = inOrder(lifeCycleListener, consulService);
    inOrder.verify(lifeCycleListener).lifeCycleStarted(any());
    inOrder.verify(consulService).register();
  }

  @Test
  public void testFailWithoutConsul() {
    AnnotationConfigWebApplicationContext aggregateCtx = new AnnotationConfigWebApplicationContext();
    aggregateCtx.register(BrokenConsulConfig.class);
    aggregateCtx.refresh();

    JettyServer jettyServer = new NabApplication(new NabServletContextConfig()).createJettyServer(aggregateCtx,
            false,
            portSupplier -> portSupplier.apply(0),
            webAppContext -> webAppContext.addLifeCycleListener(new JettyLifeCycleListener(aggregateCtx)));

    assertThrows(ConsulServiceException.class, jettyServer::start);
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
    Properties serviceProperties() {
      Properties properties = new Properties();
      properties.setProperty("consul.enabled", "true");
      properties.setProperty("serviceName", "testService");
      properties.setProperty("consul.http.port", "123");
      return properties;
    }
  }
}
