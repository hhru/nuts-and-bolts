package ru.hh.nab.testing;

import com.google.common.collect.Maps;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
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
import ru.hh.nab.Launcher;
import ru.hh.util.Classes;

public abstract class JerseyTest {
  public static class Instance {
    public final Launcher.Instance instance;
    public final String baseUrl;

    public Instance(Launcher.Instance instance) {
      this.instance = instance;
      this.baseUrl = "http://127.0.0.1:" + instance.port + "/";
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

  protected static ConcurrentMap<Class<? extends JerseyTest>, Holder<Instance>> instances = Maps.newConcurrentMap();

  static Class<? extends JerseyTest> definingSubclass(Class<? extends JerseyTest> clazz) {
    Class<? extends JerseyTest> current = clazz;
    while (true) {
      if (Classes.hasDeclaredMethod(current, "settings") ||
          Classes.hasDeclaredMethod(current, "properties") ||
          Classes.hasDeclaredMethod(current, "apiSecurity") ||
          Classes.hasDeclaredMethod(current, "limits")) {
        return current;
      }
      current = current.getSuperclass().asSubclass(JerseyTest.class);
    }
  }

  protected JerseyTest() {
    Class<? extends JerseyTest> klass = definingSubclass(this.getClass());
    Holder<Instance> newHolder = new Holder<>();
    Holder<Instance> holder = instances.putIfAbsent(klass, newHolder);
    if (holder == null) {
      holder = newHolder;
    }
    try {
      holder.get(new Callable<Instance>() {
        @Override
        public Instance call() throws IOException {
          Instance ret = new Instance(createServer());
          System.out.println("=== Test server is bound to port " + ret.instance.port + " ===");
          return ret;
        }
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected final Instance instance() {
    return instances.get(definingSubclass(this.getClass())).get();
  }

  protected Injector injector() {
    return instance().instance.injector;
  }

  protected final String baseUrl() {
    return instance().baseUrl;
  }

  protected final int port() {
    return instance().instance.port;
  }

  protected abstract Properties settings();

  protected abstract Module module();

  protected Properties apiSecurity() {
    return new Properties();
  }

  protected Properties limits() {
    return new Properties();
  }

  private Launcher.Instance createServer() throws IOException {
    return Launcher.testMode(Stage.DEVELOPMENT, module(), settings(), apiSecurity(), limits());
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
    public HttpRoute determineRoute(final HttpHost target, final HttpRequest request, final HttpContext context) throws
            HttpException {
      return super.determineRoute(target == null ? defaultHost : target, request, context);
    }
  }
}
