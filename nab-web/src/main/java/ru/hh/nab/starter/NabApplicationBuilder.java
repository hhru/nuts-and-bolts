package ru.hh.nab.starter;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

public final class NabApplicationBuilder {

  private final List<Function<WebApplicationContext, ServletContextListener>> listenerProviders;
  private final List<BiConsumer<ServletContext, WebApplicationContext>> servletContextConfigurers;
  private final List<BiConsumer<WebAppContext, WebApplicationContext>> servletContextHandlerConfigurers;

  private String contextPath;
  private ClassLoader classLoader;

  NabApplicationBuilder() {
    listenerProviders = new ArrayList<>();
    servletContextConfigurers = new ArrayList<>();
    servletContextHandlerConfigurers = new ArrayList<>();
  }

  public NabApplicationBuilder setContextPath(String contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  public NabApplicationBuilder setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  // LISTENER

  public NabApplicationBuilder addListener(ServletContextListener listener) {
    listenerProviders.add(ctx -> listener);
    return this;
  }

  public NabApplicationBuilder addListenerBean(Function<WebApplicationContext, ServletContextListener> listenerProvider) {
    listenerProviders.add(listenerProvider);
    return this;
  }

  // Spring CONTEXT

  public NabApplicationBuilder configureWebapp(BiConsumer<WebAppContext, WebApplicationContext> servletContextHandlerConfigurer) {
    this.servletContextHandlerConfigurers.add(servletContextHandlerConfigurer);
    return this;
  }

  // method for chaning
  public NabApplicationBuilder apply(Consumer<NabApplicationBuilder> operation) {
    operation.accept(this);
    return this;
  }

  // LIFECYCLE

  public NabApplicationBuilder onWebAppStarted(BiConsumer<ServletContext, WebApplicationContext> servletContextConfigurer) {
    this.servletContextConfigurers.add(servletContextConfigurer);
    return this;
  }

  public NabApplication build() {
    return new NabApplication(new NabServletContextConfig() {

      @Override
      protected ClassLoader getClassLoader() {
        if (classLoader == null) {
          return super.getClassLoader();
        }
        return classLoader;
      }

      @Override
      protected String getContextPath() {
        return StringUtils.hasLength(contextPath) ? contextPath : super.getContextPath();
      }

      @Override
      protected void configureWebapp(WebAppContext webAppContext, WebApplicationContext rootCtx) {
        servletContextHandlerConfigurers.forEach(cfg -> cfg.accept(webAppContext, rootCtx));
      }

      @Override
      protected void configureServletContext(ServletContext servletContext, WebApplicationContext rootCtx) {
        servletContextConfigurers.forEach(cfg -> cfg.accept(servletContext, rootCtx));
      }

      @Override
      protected List<ServletContextListener> getListeners(WebApplicationContext rootCtx) {
        return listenerProviders.stream().map(provider -> provider.apply(rootCtx)).collect(Collectors.toList());
      }
    });
  }
}
