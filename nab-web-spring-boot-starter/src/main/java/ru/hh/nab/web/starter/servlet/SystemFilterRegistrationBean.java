package ru.hh.nab.web.starter.servlet;

import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.core.PriorityOrdered;

/**
 * Implements {@link PriorityOrdered}, so filters registered with SystemFilterRegistrationBean always have higher priority than filters registered
 * with plain {@link FilterRegistrationBean} regardless of their order values.
 * This class is intended for internal use, and it's not recommended to use it in application code.
 */
public class SystemFilterRegistrationBean<T extends Filter> extends FilterRegistrationBean<T> implements PriorityOrdered {

  public SystemFilterRegistrationBean() {
  }

  public SystemFilterRegistrationBean(T filter, ServletRegistrationBean<?>... servletRegistrationBeans) {
    super(filter, servletRegistrationBeans);
  }
}
