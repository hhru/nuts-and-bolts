package ru.hh.nab.starter;

import jakarta.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.springframework.web.context.WebApplicationContext;

public final class NabApplicationBuilder {

  private final List<BiConsumer<ServletContext, WebApplicationContext>> servletContextConfigurers;

  NabApplicationBuilder() {
    servletContextConfigurers = new ArrayList<>();
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
      protected void configureServletContext(ServletContext servletContext, WebApplicationContext rootCtx) {
        servletContextConfigurers.forEach(cfg -> cfg.accept(servletContext, rootCtx));
      }
    });
  }
}
