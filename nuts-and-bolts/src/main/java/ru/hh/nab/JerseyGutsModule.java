package ru.hh.nab;

import com.google.common.base.Optional;
import static com.google.common.collect.Maps.newHashMap;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.GuiceComponentProviderFactory;
import com.sun.jersey.spi.container.WebApplication;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.ext.Providers;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.memory.ByteBufferManager;
import ru.hh.nab.NabModule.GrizzletDef;
import ru.hh.nab.NabModule.GrizzletDefs;
import ru.hh.nab.grizzly.DefaultCharacterEncodingHandler;
import ru.hh.nab.grizzly.GrizzletHandler;
import ru.hh.nab.grizzly.RequestDispatcher;
import ru.hh.nab.grizzly.RequestHandler;
import ru.hh.nab.grizzly.SimpleGrizzlyWebServer;
import ru.hh.nab.health.limits.Limits;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.health.monitoring.TimingsLoggerFactory;
import ru.hh.nab.jersey.HeadersAnnotationFilterFactory;
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
  protected JerseyHttpHandler jerseyAdapter(ResourceConfig resources, WebApplication wa) {
    return new JerseyHttpHandler(resources, wa);
  }

  @Provides
  @Singleton
  protected SimpleGrizzlyWebServer grizzlyWebServer(
      Settings settings, JerseyHttpHandler jersey, GrizzletDefs grizzletDefs, Limits limits, Provider<Injector> inj,
      TimingsLoggerFactory tlFactory) {
    SimpleGrizzlyWebServer ws = new SimpleGrizzlyWebServer(settings.port, settings.concurrencyLevel, tlFactory, settings.workersQueueLimit);
    ws.setCoreThreads(settings.concurrencyLevel);

    ws.addGrizzlyAdapter(new DefaultCharacterEncodingHandler());

    final Properties selectorProperties = settings.subTree("selector");

    final NetworkListener networkListener = ws.getNetworkListener();
    networkListener.getKeepAlive().setMaxRequestsCount(
        Integer.parseInt(selectorProperties.getProperty("maxKeepAliveRequests", "4096")));
    networkListener.getCompressionConfig().setCompressionMinSize(Integer.MAX_VALUE);
    networkListener.setMaxPendingBytes(
        Integer.parseInt(selectorProperties.getProperty("sendBufferSize", "32768")));
    networkListener.setMaxBufferedPostSize(
        Integer.parseInt(selectorProperties.getProperty("bufferSize", "32768")));
    networkListener.setMaxHttpHeaderSize(
        Integer.parseInt(selectorProperties.getProperty("headerSize", "16384")));
    networkListener.getTransport().setMemoryManager(new ByteBufferManager(true, 128 * 1024, ByteBufferManager.DEFAULT_SMALL_BUFFER_SIZE));
    networkListener.getTransport().setSelectorRunnersCount(1);

    List<GrizzletHandler> grizzletHandlers = Lists.newArrayListWithExpectedSize(grizzletDefs.size());
    for (GrizzletDef a : grizzletDefs) {
      RequestHandler handler = inj.get().getInstance(a.handlerClass);
      grizzletHandlers.add(new GrizzletHandler(a.handlerClass, handler, limits));
    }
    RequestDispatcher grizzletsDispatcher = new RequestDispatcher(grizzletHandlers);
    ws.addGrizzlyAdapter(grizzletsDispatcher);

    ws.addGrizzlyAdapter(jersey);

    return ws;
  }

  @Provides
  @Singleton
  protected ResourceConfig resourceConfig() {
    ResourceConfig ret = new DefaultResourceConfig();
    ret.getProperties().put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, HeadersAnnotationFilterFactory.class.getName());
    return ret;
  }

  @Provides
  @Singleton
  protected GuiceComponentProviderFactory componentProviderFactory(ResourceConfig resources, Injector inj) {
    return new GuiceComponentProviderFactory(resources, inj);
  }

  @Provides
  @Singleton
  protected WebApplication initializedWebApplication(ResourceConfig resources, GuiceComponentProviderFactory ioc) {
    webapp.initiate(resources, ioc);
    return webapp;
  }

  @Provides
  @Singleton
  protected Providers webApplicationProviders() {
    return webapp.getProviders();
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

  @Provides
  @Singleton
  protected TimingsLoggerFactory timingsLoggerFactory(Settings settings) {
    Properties timingProps = settings.subTree("timings");
    Map<String, Long> delays = newHashMap();
    for (Map.Entry<Object, Object> ent : timingProps.entrySet()) {
      delays.put(ent.getKey().toString(), Long.valueOf(ent.getValue().toString()));
    }
    Object toleranceStr = timingProps.get("tolerance");
    Optional<Long> tolerance = (toleranceStr == null) ? Optional.<Long>absent() : Optional.of(Long.valueOf(toleranceStr.toString()));
    return new TimingsLoggerFactory(delays, tolerance);
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
