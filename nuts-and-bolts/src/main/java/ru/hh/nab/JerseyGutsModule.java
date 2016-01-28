package ru.hh.nab;

import static com.google.common.collect.Maps.newHashMap;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.GuiceComponentProviderFactory;
import com.sun.jersey.spi.container.WebApplication;
import java.util.Map;
import java.util.Properties;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.ext.Providers;

import org.glassfish.grizzly.http.server.Request;
import org.slf4j.LoggerFactory;
import ru.hh.nab.grizzly.SimpleGrizzlyWebServer;
import ru.hh.nab.grizzly.monitoring.ConnectionProbeTimingLogger;
import ru.hh.nab.health.limits.Limits;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.health.monitoring.TimingsLoggerFactory;
import ru.hh.nab.jersey.JerseyResourceFilterFactory;
import ru.hh.nab.jersey.JerseyHttpHandler;
import ru.hh.nab.scopes.RequestScope;
import ru.hh.nab.security.PermissionLoader;
import ru.hh.nab.security.Permissions;

public class JerseyGutsModule extends AbstractModule {
  private final WebApplication webapp;

  public JerseyGutsModule(WebApplication webapp) {
    this.webapp = webapp;
  }

  @Override
  protected void configure() { }

  @Provides
  @Singleton
  protected JerseyHttpHandler jerseyAdapter(ResourceConfig resources,
                                            WebApplication wa,
                                            Provider<TimingsLogger> timingsLoggerProvider,
                                            Settings settings) {
    boolean allowFlush = Boolean.parseBoolean(settings.subTree("jersey").getProperty("allowFlush", "false"));
    return new JerseyHttpHandler(resources, wa, timingsLoggerProvider, allowFlush);
  }

  @Provides
  @Singleton
  protected SimpleGrizzlyWebServer grizzlyWebServer(
      Settings settings, JerseyHttpHandler jersey, Limits limits, Provider<Injector> inj,
      TimingsLoggerFactory tlFactory) {

    SimpleGrizzlyWebServer grizzlyServer = SimpleGrizzlyWebServer.create(settings, tlFactory,
        new ConnectionProbeTimingLogger(LoggerFactory.getLogger(TimingsLogger.class)));

    grizzlyServer.addGrizzlyAdapter(jersey);

    return grizzlyServer;
  }

  @Provides
  @Singleton
  protected ResourceConfig resourceConfig(Settings settings) {
    ResourceConfig ret = new DefaultResourceConfig();
    ret.getProperties().put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, JerseyResourceFilterFactory.class.getName());

    boolean disableWadl = Boolean.parseBoolean(settings.subTree("jersey").getProperty("disableWadl", "true"));
    ret.getFeatures().put(ResourceConfig.FEATURE_DISABLE_WADL, disableWadl);

    return ret;
  }

  @Provides
  @Singleton
  protected GuiceComponentProviderFactory componentProviderFactory(ResourceConfig resources, Injector inj) {
    return new GuiceComponentProviderFactory(resources, inj);
  }

  private WebApplication getInitiatedWebapp(ResourceConfig resources, GuiceComponentProviderFactory ioc) {
    synchronized (webapp) {
      if (!webapp.isInitiated()) {
        webapp.initiate(resources, ioc);
      }
    }
    return webapp;
  }

  @Provides
  @Singleton
  protected WebApplication initializedWebApplication(ResourceConfig resources, GuiceComponentProviderFactory ioc) {
    return getInitiatedWebapp(resources, ioc);
  }

  @Provides
  @Singleton
  protected Providers webApplicationProviders(ResourceConfig resources, GuiceComponentProviderFactory ioc) {
    return getInitiatedWebapp(resources, ioc).getProviders();
  }

  @Provides
  @RequestScoped
  Request httpRequestContext() {
    Object request = RequestScope.currentRequest();
    if (request == null || !(request instanceof Request)) {
      throw new IllegalArgumentException("Not a grizzly request");
    }
    return (Request) request;
  }

  @Provides
  protected RequestScope.RequestScopeClosure requestScopeClosure() {
    return RequestScope.currentClosure();
  }

  private static Map<String, Long> getDelays(Settings settings) {
    Map<String, Long> delays = newHashMap();
    Properties delaysProps = settings.subTree("timings.delays");
    if (delaysProps != null) {
      for (Map.Entry<Object, Object> ent : delaysProps.entrySet()) {
        delays.put(ent.getKey().toString(), Long.valueOf(ent.getValue().toString()));
      }
    }
    return delays;
  }

  @Provides
  @Singleton
  protected TimingsLoggerFactory timingsLoggerFactory(Settings settings) {
    return new TimingsLoggerFactory(getDelays(settings));
  }

  @Provides
  @RequestScoped
  protected TimingsLogger timingsLogger() {
    return RequestScope.currentTimingsLogger();
  }

  @Provides
  @RequestScoped
  protected Permissions permissions(Request req, PermissionLoader permissions) {
    String apiKey = req.getHeader("X-Hh-Api-Key");
    Permissions ret = permissions.forKey(apiKey);
    if (ret != null) {
      return permissions.forKey(apiKey);
    }
    return permissions.anonymous();
  }
}
