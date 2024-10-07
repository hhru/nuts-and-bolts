package ru.hh.nab.web.spring;

import java.util.Collection;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;

public class NabAnnotationConfigServletWebServerApplicationContext extends AnnotationConfigServletWebServerApplicationContext {

  @Override
  protected Collection<ServletContextInitializer> getServletContextInitializerBeans() {
    return new NabServletContextInitializerBeans(getBeanFactory());
  }
}
