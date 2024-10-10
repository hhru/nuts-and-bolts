package ru.hh.nab.web.spring;

import java.util.Collection;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

public class NabServletWebServerApplicationContext extends ServletWebServerApplicationContext {

  @Override
  protected Collection<ServletContextInitializer> getServletContextInitializerBeans() {
    return new NabServletContextInitializerBeans(getBeanFactory());
  }
}
