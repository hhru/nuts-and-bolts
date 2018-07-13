package ru.hh.nab.testbase;

import java.util.concurrent.ConcurrentHashMap;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.hh.nab.starter.servlet.DefaultServletConfig;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.starter.servlet.ServletConfig;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;

/**
 * Launches Jetty instance with application context provided by {@link AbstractJUnit4SpringContextTests}
 * and servlet config provided by {@link #servletConfig()} on a random port before test methods start to execute.
 * For some examples see nab-starter-tests module.
 */
public abstract class JettyStarterTestBase extends AbstractJUnit4SpringContextTests {

  private static final Logger LOGGER = LoggerFactory.getLogger(JettyStarterTestBase.class);
  private static final ConcurrentMap<Class<? extends JettyStarterTestBase>, Holder<Instance>> INSTANCES = new ConcurrentHashMap<>();

  @Before
  public void setUp() {
    Holder<Instance> newHolder = new Holder<>();
    Holder<Instance> holder = INSTANCES.putIfAbsent(getClass(), newHolder);
    if (holder == null) {
      holder = newHolder;
    }
    try {
      holder.get(() -> {
        int port = NabApplication.startJettyServer(applicationContext, servletConfig());
        LOGGER.info("Test server is bound to port {}", port);
        return new Instance(port);
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Override to provide custom servlet config for Jetty instance
   */
  protected ServletConfig servletConfig() {
    return new DefaultServletConfig();
  }

  private Instance instance() {
    return INSTANCES.get(getClass()).get();
  }

  protected String baseUrl() {
    return instance().baseUrl;
  }

  protected int port() {
    return instance().port;
  }

  public static class Instance {
    final String baseUrl;
    final int port;

    Instance(int port) {
      this.port = port;
      this.baseUrl = String.format("http://127.0.0.1:%d", port);
    }
  }

  protected CloseableHttpClient httpClient() {
    return HttpClientBuilder.create()
        .setDefaultRequestConfig(
            RequestConfig.custom().setRedirectsEnabled(false).setCookieSpec(CookieSpecs.STANDARD).build())
        .setRoutePlanner(new DefaultRoutePlannerImpl(port()))
        .build();
  }

  private static class DefaultRoutePlannerImpl extends DefaultRoutePlanner {
    private final HttpHost defaultHost;

    DefaultRoutePlannerImpl(final int port) {
      super(null);
      this.defaultHost = new HttpHost("127.0.0.1", port);
    }

    @Override
    public HttpRoute determineRoute(final HttpHost target, final HttpRequest request, final HttpContext context) throws HttpException {
      return super.determineRoute(target == null ? defaultHost : target, request, context);
    }
  }

  private static class Holder<T> {
    private T t;

    public synchronized T get(Callable<T> tProvider) throws Exception {
      if (t == null) {
        t = tProvider.call();
      }
      return t;
    }

    public synchronized T get() {
      return t;
    }
  }
}
