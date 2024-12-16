package ru.hh.nab.web.starter.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import java.util.Collection;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

public class RegistrationValidator implements ServletContextInitializer {

  private final ListableBeanFactory beanFactory;

  public RegistrationValidator(ListableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void onStartup(ServletContext servletContext) {
    for (ServletContextInitializer servletContextInitializer : new ServletContextInitializerBeans(beanFactory)) {
      if (servletContextInitializer instanceof ServletRegistrationBean<?> servletRegistrationBean) {
        validateServletRegistration(servletRegistrationBean, servletContext.getServletRegistration(servletRegistrationBean.getServletName()));
      }
    }
  }

  private void validateServletRegistration(ServletRegistrationBean<?> servletRegistrationBean, ServletRegistration servletRegistration) {
    Collection<String> mappings = servletRegistration.getMappings();
    if (mappings.isEmpty()) {
      throw new IllegalArgumentException("Servlet %s has conflicting mappings".formatted(servletRegistrationBean));
    }
  }
}
