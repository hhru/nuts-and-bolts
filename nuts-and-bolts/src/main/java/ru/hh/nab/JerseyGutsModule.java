package ru.hh.nab;

import com.google.common.base.Optional;
import static com.google.common.collect.Maps.newHashMap;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.RequestScoped;
import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.GuiceComponentProviderFactory;
import com.sun.jersey.spi.container.WebApplication;
import java.util.Map;
import java.util.Properties;
import ru.hh.nab.NabModule.GrizzletDef;
import ru.hh.nab.NabModule.GrizzletDefs;
import ru.hh.nab.NabModule.ServletDef;
import ru.hh.nab.NabModule.ServletDefs;
import ru.hh.nab.grizzly.Concurrency;
import ru.hh.nab.grizzly.HandlerDecorator;
import ru.hh.nab.grizzly.HttpMethod;
import ru.hh.nab.grizzly.RequestDispatcher;
import ru.hh.nab.grizzly.RequestHandler;
import ru.hh.nab.grizzly.Route;
import ru.hh.nab.grizzly.Router;
import ru.hh.nab.grizzly.SimpleGrizzlyWebServer;
import ru.hh.nab.health.limits.Limits;
import ru.hh.nab.health.monitoring.TimingsLogger;
import ru.hh.nab.health.monitoring.TimingsLoggerFactory;
import ru.hh.nab.jersey.HeadersAnnotationFilterFactory;
import ru.hh.nab.jersey.NabGrizzlyContainer;
import ru.hh.nab.scopes.RequestScope;
import ru.hh.nab.security.PermissionLoader;
import ru.hh.nab.security.Permissions;

public class JerseyGutsModule extends AbstractModule {
  private final WebApplication webapp;

  public JerseyGutsModule(WebApplication webapp) {
    this.webapp = webapp;
  }

  @Override
  protected void configure() {
  }

  protected
  @Provides
  @Singleton
  NabGrizzlyContainer nabGrizzlyContainer(
          ResourceConfig resources, WebApplication wa) {
    NabGrizzlyContainer ret = new NabGrizzlyContainer(resources, wa);
    ret.setHandleStaticResources(false);
    return ret;
  }

  protected
  @Provides
  @Singleton
  SimpleGrizzlyWebServer grizzlyWebServer(
          Settings settings, NabGrizzlyContainer jersey, ServletDefs servlets,
          GrizzletDefs grizzlets, Limits limits, Provider<Injector> inj, TimingsLoggerFactory tlFactory) {
    SimpleGrizzlyWebServer ws = new SimpleGrizzlyWebServer(settings.port, settings.concurrencyLevel, tlFactory);
    ws.setCoreThreads(settings.concurrencyLevel);
    SelectorThread selector = ws.getSelectorThread();
    selector.setMaxKeepAliveRequests(4096);
    selector.setCompressionMinSize(Integer.MAX_VALUE);
    selector.setSendBufferSize(4096);
    selector.setBufferSize(4096);
    selector.setSelectorReadThreadsCount(1);
    selector.setUseDirectByteBuffer(true);
    selector.setUseByteBufferView(true);

    for (ServletDef s : servlets)
      ws.addGrizzlyAdapter(new ServletAdapter(inj.get().getInstance(s.servlet)));

    Router router = new Router();
    for (GrizzletDef a : grizzlets) {
      RequestHandler handler = inj.get().getInstance(a.handlerClass);
      router.addRouting(extractPath(a.handlerClass),
              new HandlerDecorator(handler, extractMethods(a.handlerClass),
                      limits.compoundLimit(extractConcurrency(a.handlerClass))));
    }
    ws.addGrizzlyAdapter(new RequestDispatcher(router));

    ws.addGrizzlyAdapter(jersey);

    return ws;
  }

  private static Route maybeGetRoute(Class<? extends RequestHandler> handler) {
    Route route = handler.getAnnotation(Route.class);
    if (route == null)
      throw new IllegalArgumentException("@Route in " + handler.getSimpleName() + "?");
    return route;
  }

  private static HttpMethod[] extractMethods(Class<? extends RequestHandler> handler) {
    return maybeGetRoute(handler).methods();
  }

  private static String extractPath(Class<? extends RequestHandler> handler) {
    return maybeGetRoute(handler).path();
  }

  private static String[] extractConcurrency(Class<? extends RequestHandler> handler) {
    Concurrency concurrency = handler.getAnnotation(Concurrency.class);
    if (concurrency == null)
      return new String[] {"global"};
    return concurrency.value();
  }

  protected
  @Provides
  @Singleton
  ResourceConfig resourceConfig() {
    ResourceConfig ret = new DefaultResourceConfig();
    ret.getProperties().put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES,
            HeadersAnnotationFilterFactory.class.getName());
    return ret;
  }

  protected
  @Provides
  @Singleton
  GuiceComponentProviderFactory componentProviderFactory(
          ResourceConfig resources, Injector inj) {
    return new GuiceComponentProviderFactory(resources, inj);
  }

  protected
  @Provides
  @Singleton
  WebApplication initializedWebApplication(ResourceConfig resources, GuiceComponentProviderFactory ioc) {
    webapp.initiate(resources, ioc);
    return webapp;
  }

  protected
  @Provides
  @RequestScoped
  GrizzlyRequest httpRequestContext() {
    Object request = RequestScope.currentRequest();
    if (request == null || !(request instanceof GrizzlyRequest)) {
      throw new IllegalArgumentException("Not a grizzly request");
    }
    return (GrizzlyRequest) request;
  }

  protected
  @Provides
  RequestScope.RequestScopeClosure requestScopeClosure() {
    return RequestScope.currentClosure();
  }

  protected
  @Provides
  @Singleton
  TimingsLoggerFactory timingsLoggerFactory(Settings settings) {
    Properties timingProps = settings.subTree("timings");
    Map<String, Long> delays = newHashMap();
    for (Map.Entry<Object, Object> ent : timingProps.entrySet())
      delays.put(ent.getKey().toString(), Long.valueOf(ent.getValue().toString()));
    Object toleranceStr = timingProps.get("tolerance");
    Optional<Long> tolerance = (toleranceStr == null)
        ? Optional.<Long>absent()
        : Optional.of(Long.valueOf(toleranceStr.toString()));
    return new TimingsLoggerFactory(delays, tolerance);
  }

  protected
  @Provides
  @RequestScoped
  TimingsLogger timingsLogger() {
    return RequestScope.currentTimingsLogger();
  }

  protected
  @Provides
  @RequestScoped
  Permissions permissions(GrizzlyRequest req, PermissionLoader permissions) {
    String apiKey = req.getHeader("X-Hh-Api-Key");
    Permissions ret = permissions.forKey(apiKey);
    if (ret != null)
      return permissions.forKey(apiKey);
    return permissions.anonymous();
  }
}
