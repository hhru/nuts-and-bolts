package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.servlet.RequestScoped;
import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.GuiceComponentProviderFactory;
import com.sun.jersey.spi.container.WebApplication;
import javax.ws.rs.Path;
import ru.hh.nab.NabModule.ServletDef;
import ru.hh.nab.NabModule.ServletDefs;
import ru.hh.nab.jersey.HeadersAnnotationFilterFactory;
import ru.hh.nab.jersey.HttpRequestMDCDecorator;
import ru.hh.nab.jersey.NabGrizzlyContainer;
import ru.hh.nab.jersey.WebMethodMatcher;

public class JerseyGutsModule extends AbstractModule {
  private final WebApplication webapp;

  public JerseyGutsModule(WebApplication webapp) {
    this.webapp = webapp;
  }

  @Override
  protected void configure() {
    bindInterceptor(Matchers.annotatedWith(Path.class), new WebMethodMatcher(),
            new HttpRequestMDCDecorator(getProvider(GrizzlyRequest.class)));
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
  GrizzlyRequest grizzlyRequest(NabGrizzlyContainer grizzly) {
    return grizzly.getGrizzlyRequest();
  }

  protected
  @Provides
  @Singleton
  GrizzlyWebServer grizzlyWebServer(
          Settings settings, NabGrizzlyContainer jersey, ServletDefs servlets,
          Provider<Injector> inj) {
    GrizzlyWebServer ws = new GrizzlyWebServer(settings.port);
    ws.setCoreThreads(settings.concurrencyLevel);
    ws.setMaxThreads(settings.concurrencyLevel);
    SelectorThread selector = ws.getSelectorThread();
    selector.setMaxKeepAliveRequests(4096);
    selector.setCompressionMinSize(Integer.MAX_VALUE);
    selector.setSendBufferSize(4096);
    selector.setBufferSize(4096);
    selector.setSelectorReadThreadsCount(1);
    selector.setUseDirectByteBuffer(true);
    selector.setUseByteBufferView(true);

    ws.addGrizzlyAdapter(jersey, new String[]{"/*"});

    for (ServletDef s : servlets)
      ws.addGrizzlyAdapter(new ServletAdapter(inj.get().getInstance(s.servlet)),
              new String[]{s.pattern});

    return ws;
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
}
