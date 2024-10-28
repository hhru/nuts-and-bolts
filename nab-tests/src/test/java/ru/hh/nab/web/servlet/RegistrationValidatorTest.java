package ru.hh.nab.web.servlet;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import java.io.IOException;
import java.util.regex.Pattern;
import org.eclipse.jetty.servlet.DefaultServlet;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.servlet.ServletFilterPriorities;
import ru.hh.nab.common.servlet.SystemFilter;
import ru.hh.nab.starter.NabAppTestConfig;

public class RegistrationValidatorTest {

  @Test
  public void testValidServletsValidation() {
    assertDoesNotThrow(() -> {
      ConfigurableApplicationContext context = SpringApplication.run(ValidServletConfiguration.class);
      SpringApplication.exit(context);
    });
  }

  @Test
  public void testConflictingServletsValidation() {
    ApplicationContextException exception = assertThrows(
        ApplicationContextException.class,
        () -> SpringApplication.run(ConflictingServletConfiguration.class)
    );
    assertNotNull(exception.getRootCause());
    assertTrue(
        Pattern
            .compile("Servlet conflictingServlet2.*/conflict.*has conflicting mappings.*")
            .matcher(exception.getRootCause().getMessage())
            .matches()
    );
  }

  @Test
  public void testValidFiltersValidation() {
    assertDoesNotThrow(() -> {
      ConfigurableApplicationContext context = SpringApplication.run(ValidFilterConfiguration.class);
      SpringApplication.exit(context);
    });
  }

  @Test
  public void testInvalidApplicationFilterOrderValidation() {
    ApplicationContextException exception = assertThrows(
        ApplicationContextException.class,
        () -> SpringApplication.run(InvalidApplicationFilterOrderConfiguration.class)
    );
    assertNotNull(exception.getRootCause());
    assertTrue(
        Pattern
            .compile("Filter testFilter.*order=-3000.*has invalid order.*")
            .matcher(exception.getRootCause().getMessage())
            .matches()
    );
  }

  @Test
  public void testInvalidSystemFilterOrderValidation() {
    ApplicationContextException exception = assertThrows(
        ApplicationContextException.class,
        () -> SpringApplication.run(InvalidSystemFilterOrderConfiguration.class)
    );
    assertNotNull(exception.getRootCause());
    assertTrue(
        Pattern
            .compile("System filter testSystemFilter.*order=5000.*has invalid order.*")
            .matcher(exception.getRootCause().getMessage())
            .matches()
    );
  }

  @Configuration
  @Import(NabAppTestConfig.class)
  @EnableAutoConfiguration
  public static class ValidServletConfiguration {
    @Bean
    ServletRegistrationBean<HttpServlet> servlet1() {
      return new ServletRegistrationBean<>(new DefaultServlet(), "/test1");
    }

    @Bean
    ServletRegistrationBean<HttpServlet> servlet2() {
      return new ServletRegistrationBean<>(new DefaultServlet(), "/test2");
    }
  }

  @Configuration
  @Import(NabAppTestConfig.class)
  @EnableAutoConfiguration
  public static class ConflictingServletConfiguration {

    private static final String CONFLICTING_URL = "/conflict";

    @Bean
    ServletRegistrationBean<HttpServlet> conflictingServlet1() {
      ServletRegistrationBean<HttpServlet> registration = new ServletRegistrationBean<>(new DefaultServlet(), CONFLICTING_URL);
      registration.setOrder(ServletFilterPriorities.USER);
      return registration;
    }

    @Bean
    ServletRegistrationBean<HttpServlet> conflictingServlet2() {
      ServletRegistrationBean<HttpServlet> registration = new ServletRegistrationBean<>(new DefaultServlet(), CONFLICTING_URL);
      registration.setOrder(ServletFilterPriorities.USER + 1);
      return registration;
    }
  }

  @Configuration
  @Import(NabAppTestConfig.class)
  @EnableAutoConfiguration
  public static class ValidFilterConfiguration {
    @Bean
    FilterRegistrationBean<TestFilter> testFilter() {
      FilterRegistrationBean<TestFilter> registration = new FilterRegistrationBean<>(new TestFilter());
      registration.setOrder(ServletFilterPriorities.USER);
      return registration;
    }

    @Bean
    FilterRegistrationBean<TestSystemFilter> testSystemFilter() {
      FilterRegistrationBean<TestSystemFilter> registration = new FilterRegistrationBean<>(new TestSystemFilter());
      registration.setOrder(ServletFilterPriorities.SYSTEM_HEADER_DECORATOR);
      return registration;
    }
  }

  @Configuration
  @Import(NabAppTestConfig.class)
  @EnableAutoConfiguration
  public static class InvalidApplicationFilterOrderConfiguration {
    @Bean
    FilterRegistrationBean<TestFilter> testFilter() {
      FilterRegistrationBean<TestFilter> registration = new FilterRegistrationBean<>(new TestFilter());
      registration.setOrder(ServletFilterPriorities.SYSTEM_HEADER_DECORATOR);
      return registration;
    }
  }

  @Configuration
  @Import(NabAppTestConfig.class)
  @EnableAutoConfiguration
  public static class InvalidSystemFilterOrderConfiguration {
    @Bean
    FilterRegistrationBean<TestSystemFilter> testSystemFilter() {
      FilterRegistrationBean<TestSystemFilter> registration = new FilterRegistrationBean<>(new TestSystemFilter());
      registration.setOrder(ServletFilterPriorities.USER);
      return registration;
    }
  }

  private static class TestFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      chain.doFilter(request, response);
    }
  }

  private static class TestSystemFilter implements Filter, SystemFilter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      chain.doFilter(request, response);
    }
  }
}
