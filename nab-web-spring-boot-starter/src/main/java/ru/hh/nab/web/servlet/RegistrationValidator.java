package ru.hh.nab.web.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import java.util.Collection;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import ru.hh.nab.common.servlet.ServletFilterPriorities;
import ru.hh.nab.common.servlet.SystemFilter;

public class RegistrationValidator implements ServletContextInitializer {

  private final ListableBeanFactory beanFactory;

  public RegistrationValidator(ListableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void onStartup(ServletContext servletContext) {
    for (ServletContextInitializer servletContextInitializer : new ServletContextInitializerBeans(beanFactory)) {
      if (servletContextInitializer instanceof FilterRegistrationBean<?> filterRegistrationBean) {
        validateFilterRegistration(filterRegistrationBean);
      } else if (servletContextInitializer instanceof ServletRegistrationBean<?> servletRegistrationBean) {
        validateServletRegistration(servletRegistrationBean, servletContext.getServletRegistration(servletRegistrationBean.getServletName()));
      }
    }
  }

  private void validateFilterRegistration(FilterRegistrationBean<?> filterRegistrationBean) {
    if (filterRegistrationBean.getOrder() <= ServletFilterPriorities.SYSTEM_LOWEST_PRIORITY &&
        !(filterRegistrationBean.getFilter() instanceof SystemFilter)) {
      throw new IllegalArgumentException("Filter %s has invalid order. Priorities pool [%s, %s] is reserved for system filters. See %s".formatted(
          filterRegistrationBean,
          ServletFilterPriorities.SYSTEM_HIGHEST_PRIORITY,
          ServletFilterPriorities.SYSTEM_LOWEST_PRIORITY,
          ServletFilterPriorities.class
      ));
    }
    if (filterRegistrationBean.getOrder() > ServletFilterPriorities.SYSTEM_LOWEST_PRIORITY &&
        filterRegistrationBean.getFilter() instanceof SystemFilter) {
      throw new IllegalArgumentException(
          "System filter %s has invalid order. System filters should have priority from reserved priorities pool [%s, %s]. See %s".formatted(
              filterRegistrationBean,
              ServletFilterPriorities.SYSTEM_HIGHEST_PRIORITY,
              ServletFilterPriorities.SYSTEM_LOWEST_PRIORITY,
              ServletFilterPriorities.class
          ));
    }
  }

  private void validateServletRegistration(ServletRegistrationBean<?> servletRegistrationBean, ServletRegistration servletRegistration) {
    Collection<String> mappings = servletRegistration.getMappings();
    if (mappings.isEmpty()) {
      throw new IllegalArgumentException("Servlet %s has conflicting mappings".formatted(servletRegistrationBean));
    }
  }
}
