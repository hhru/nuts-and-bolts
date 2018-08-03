package ru.hh.nab.testbase;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.OK;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import static org.junit.Assert.assertEquals;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import static ru.hh.nab.starter.NabApplication.configureLogger;
import ru.hh.nab.starter.jersey.DefaultResourceConfig;
import ru.hh.nab.starter.servlet.DefaultServletConfig;
import ru.hh.nab.starter.servlet.ServletConfig;

public abstract class NabJerseyTestBase extends JerseyTest {

  private static AnnotationConfigWebApplicationContext context;
  private static final Object MUTEX = new Object();

  @Override
  protected DeploymentContext configureDeployment() {
    configureLogger();

    synchronized (MUTEX) {
      if (context == null) {
        context = configureSpringContext();
      }
    }

    DeploymentContext context = ServletDeploymentContext.builder(configure()).build();
    context.getResourceConfig().property("contextConfig", context);
    return context;
  }

  @Override
  protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
    return new JettyTestContainerFactory(context, getServletConfig(), getClass());
  }

  @Override
  protected ResourceConfig configure() {
    return new DefaultResourceConfig();
  }

  protected void assertGet(String url, String expectedResponse) {
    Response response = target(url).request().get();

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals(expectedResponse, response.readEntity(String.class));
  }

  protected void assertGet(Invocation.Builder request, String expectedResponse) {
    Response response = request.get();

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals(expectedResponse, response.readEntity(String.class));
  }

  protected Invocation.Builder createRequest(String url) {
    return target(url).request();
  }

  protected Response executeGet(String url) {
    return createRequest(url).get();
  }

  protected void configureSpringContext(AnnotationConfigRegistry springContext) {
  }

  private AnnotationConfigWebApplicationContext configureSpringContext() {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.register(NabTestConfig.class);
    configureSpringContext(context);
    context.refresh();
    return context;
  }

  /**
   * Override to provide custom servlet config for Jetty instance
   */
  protected ServletConfig getServletConfig() {
    return new DefaultServletConfig();
  }

  protected WebApplicationContext getContext() {
    return context;
  }

  protected <T> T getBean(Class<T> requiredType) {
    return context.getBean(requiredType);
  }

  protected <T> T getBean(String beanName, Class<T> requiredType) {
    return context.getBean(beanName, requiredType);
  }
}
