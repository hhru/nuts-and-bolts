package ru.hh.nab.testbase;

import javax.inject.Inject;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import ru.hh.nab.starter.NabApplication;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static ru.hh.nab.starter.NabApplication.configureLogger;

/**
 * Launches Jetty instance with application context provided by {@link AbstractJUnit4SpringContextTests}
 * and servlet config provided by {@link #getApplication()} on a random port before test methods start to execute.
 * For some examples see nab-tests module.
 */
@WebAppConfiguration
@RunWith(NabRunner.class)
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
}
