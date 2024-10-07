package ru.hh.nab.web.spring;

import org.springframework.aot.AotDetector;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class NabApplicationContextFactory implements ApplicationContextFactory {

  // copy of org.springframework.boot.web.servlet.context.ServletWebServerApplicationContextFactory#create method
  // that returns custom application contexts
  @Override
  public ConfigurableApplicationContext create(WebApplicationType webApplicationType) {
    return (webApplicationType != WebApplicationType.SERVLET) ? null : createContext();
  }

  private ConfigurableApplicationContext createContext() {
    if (!AotDetector.useGeneratedArtifacts()) {
      return new NabAnnotationConfigServletWebServerApplicationContext();
    }
    return new NabServletWebServerApplicationContext();
  }
}
