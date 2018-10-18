package ru.hh.nab.testbase;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.Before;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;
import static ru.hh.nab.starter.NabApplication.configureLogger;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.web.context.support.GenericWebApplicationContext;
import ru.hh.nab.starter.NabServletContextConfig;
import ru.hh.nab.testbase.JettyTestContainerFactory.JettyTestContainer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Launches Jetty instance with application context provided by {@link AbstractJUnit4SpringContextTests}
 * and servlet config provided by {@link #getServletConfig()} on a random port before test methods start to execute.
 * For some examples see nab-tests module.
 */
@WebAppConfiguration
@TestExecutionListeners(NabTestBase.TestInstanceInjectionListener.class)
public abstract class NabTestBase extends AbstractJUnit4SpringContextTests {
  private JettyTestContainer testContainer;
  private Client client;

  @Before
  public void setUpNabTestBase() {
    configureLogger();
    JettyTestContainerFactory containerFactory = new JettyTestContainerFactory(applicationContext, getServletConfig(), getClass());
    testContainer = containerFactory.createTestContainer();
    client = getClientBuilder().build();
  }

  protected ClientBuilder getClientBuilder() {
    return ClientBuilder.newBuilder();
  }

  /**
   * Override to provide custom servlet config for Jetty instance
   */
  protected NabServletContextConfig getServletConfig() {
    return new NabServletContextConfig();
  }

  protected String baseUrl() {
    return testContainer.getBaseUrl();
  }

  protected int port() {
    return testContainer.getPort();
  }

  protected void assertGet(String url, String expectedResponse) {
    Response response = createRequest(url).get();

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals(expectedResponse, response.readEntity(String.class));
  }

  protected void assertGet(Invocation.Builder request, String expectedResponse) {
    Response response = request.get();

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals(expectedResponse, response.readEntity(String.class));
  }

  protected Response executeGet(String path) {
    return createRequest(path).get();
  }

  protected String jerseyUrl(String path, Object... values) {
    return UriBuilder.fromPath(path).build(values).toString();
  }

  protected Invocation.Builder createRequest(String url) {
    return target(url).request();
  }

  protected WebTarget target(String url) {
    return client.target(baseUrl() + url);
  }

  protected Invocation.Builder createRequestFromAbsoluteUrl(String absoluteUrl) {
    return client.target(absoluteUrl).request();
  }

  public interface NabTestContext {
    int port();
    String baseUrl();
  }

  public static final class ContextInjectionAnnotationConfigWebContextLoader extends AnnotationConfigWebContextLoader {

    private static volatile ConcurrentMap<Class<? extends NabTestBase>, NabTestBase> instances;

    public ContextInjectionAnnotationConfigWebContextLoader() {
      if (instances == null) {
        synchronized (ContextInjectionAnnotationConfigWebContextLoader.class) {
          if (instances == null) {
            instances = new ConcurrentHashMap<>();
          }
        }
      }
    }

    private static void setTestInstance(NabTestBase testInstance) {
      if (instances != null) {
        instances.put(testInstance.getClass(), testInstance);
      }
    }

    @Override
    protected void loadBeanDefinitions(GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig) {
      super.loadBeanDefinitions(context, webMergedConfig);
      context.registerBean(NabTestContext.class, () -> new NabTestContext() {
        final Class<?> cls = webMergedConfig.getTestClass();
        @Override
        public int port() {
          return ofNullable(instances.get(cls)).map(NabTestBase::port)
            .orElseThrow(() -> new IllegalStateException("Test instance is not injected"));
        }

        @Override
        public String baseUrl() {
          return ofNullable(instances.get(cls)).map(NabTestBase::baseUrl)
            .orElseThrow(() -> new IllegalStateException("Test instance is not injected"));
        }
      });
    }
  }

  static final class TestInstanceInjectionListener implements TestExecutionListener {
    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception {
      NabTestBase testInstance = (NabTestBase) testContext.getTestInstance();
      ContextInjectionAnnotationConfigWebContextLoader.setTestInstance(testInstance);
    }
  }
}
