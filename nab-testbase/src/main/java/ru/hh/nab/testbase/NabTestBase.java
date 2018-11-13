package ru.hh.nab.testbase;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.springframework.beans.BeanUtils;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import static ru.hh.nab.starter.NabApplication.configureLogger;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.test.context.web.WebTestContextBootstrapper;
import ru.hh.nab.starter.NabApplication;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Launches Jetty instance with application context provided by {@link AbstractJUnit4SpringContextTests}
 * and servlet config provided by {@link #getApplication()} on a random port before test methods start to execute.
 * For some examples see nab-tests module.
 */
@WebAppConfiguration
@RunWith(NabTestBase.NabRunner.class)
@BootstrapWith(NabTestBase.NabBootstrapper.class)
public abstract class NabTestBase extends AbstractJUnit4SpringContextTests {
  @Inject
  private JettyTestContainer testContainer;
  private Client client;

  @Before
  public void setUpNabTestBase() {
    configureLogger();
    client = getClientBuilder().build();
  }

  protected ClientBuilder getClientBuilder() {
    return ClientBuilder.newBuilder();
  }

  /**
   * Override to provide custom servlet config for Jetty instance
   */
  protected NabApplication getApplication() {
    return NabApplication.builder().build();
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

  public static class NabRunner extends SpringJUnit4ClassRunner {

    private static final ThreadLocal<NabTestBase> INSTANCES = new ThreadLocal<>();

    public NabRunner(Class<?> clazz) throws InitializationError {
      super(clazz);
    }

    @Override
    protected Object createTest() throws Exception {
      Object testInstance = BeanUtils.instantiateClass(getTestClass().getOnlyConstructor());
      INSTANCES.set((NabTestBase) testInstance);
      getTestContextManager().prepareTestInstance(testInstance);
      return testInstance;
    }

    @Override
    protected void runChild(FrameworkMethod frameworkMethod, RunNotifier notifier) {
      super.runChild(frameworkMethod, notifier);
      INSTANCES.set(null);
    }

    public static NabTestBase get() {
      return INSTANCES.get();
    }
  }

  static class NabBootstrapper extends WebTestContextBootstrapper {
    @Override
    protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
      return new NabMergedContextConfiguration(super.processMergedContextConfiguration(mergedConfig));
    }
  }

  static class NabMergedContextConfiguration extends WebMergedContextConfiguration {

    NabMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
      super(mergedConfig, null);
    }
  }
}
