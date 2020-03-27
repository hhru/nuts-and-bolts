package ru.hh.nab.starter;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.exceptions.ConsulServiceException;
import ru.hh.nab.starter.jersey.TestResource;
import ru.hh.nab.starter.server.jetty.JettyLifeCycleListener;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.testbase.NabTestConfig;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext;

import java.util.Properties;

public class NabApplicationTest {

  @Rule
  public final ExpectedSystemExit exit = ExpectedSystemExit.none();

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
            mock -> mock.apply(null),
            v -> v.addLifeCycleListener(lifeCycleListener)
            );

    ConsulService consulService = aggregateCtx.getBean(ConsulService.class);

    jettyServer.start();

    InOrder inOrder = inOrder(lifeCycleListener, consulService);
    inOrder.verify(lifeCycleListener).lifeCycleStarted(any());
    inOrder.verify(consulService).register();
  }

  @Test(expected = ConsulServiceException.class)
  public void testFailWithoutConsul() {
    AnnotationConfigWebApplicationContext aggregateCtx = new AnnotationConfigWebApplicationContext();
    aggregateCtx.register(BrokenConsul.class);
    aggregateCtx.refresh();

    JettyServer jettyServer = new NabApplication(new NabServletContextConfig()).createJettyServer(aggregateCtx,
            false,
            mock -> mock.apply(0),
            v -> v.addLifeCycleListener(new JettyLifeCycleListener(aggregateCtx))
            );

    jettyServer.start();
  }

  @Test
  public void runShouldFailOnServletMappingConflict() {
    exit.expectSystemExitWithStatus(1);
    NabApplication.builder()
      .addServlet(ctx -> new DefaultServlet()).setServletName("conflictingServlet").bindTo("/status")
      .build().run(NabTestConfig.class);
  }

  @Test
  public void runShouldFailOnContextRefreshFail() {
    exit.expectSystemExitWithStatus(1);
    NabApplication.runWebApp(new NabServletContextConfig(), NabTestConfig.class, BrokenCtx.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void runShouldFailOnWrongJerseyCfg() {
    NabApplication.builder().configureJersey().registerResources(TestResource.class).bindToRoot().build().run();
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
  public static class BrokenConsul {

    @Bean
    @Primary
    FileSettings fileSettings() {
      Properties properties = new Properties();
      properties.put("consul.enabled", true);
      properties.put("serviceName", "testService");
      return new FileSettings(properties);
    }
  }
}
