package ru.hh.nab.web.starter.servlet;

import jakarta.servlet.http.HttpServlet;
import java.util.regex.Pattern;
import org.eclipse.jetty.servlet.DefaultServlet;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.servlet.ServletFilterPriorities;

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

  @Configuration
  @Import(TestWebServerFactoryCustomizer.class)
  @ImportAutoConfiguration(ServletWebServerFactoryAutoConfiguration.class)
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
  @Import(TestWebServerFactoryCustomizer.class)
  @ImportAutoConfiguration(ServletWebServerFactoryAutoConfiguration.class)
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

  private static class TestWebServerFactoryCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

    private final ListableBeanFactory beanFactory;

    private TestWebServerFactoryCustomizer(ListableBeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }

    @Override
    public void customize(JettyServletWebServerFactory factory) {
      factory.addInitializers(new RegistrationValidator(beanFactory));
    }
  }
}
