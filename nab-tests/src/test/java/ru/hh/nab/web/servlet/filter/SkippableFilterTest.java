package ru.hh.nab.web.servlet.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.web.ResourceHelper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SkippableFilterTest {

  private final ResourceHelper resourceHelper;

  public SkippableFilterTest(@LocalServerPort int serverPort) {
    this.resourceHelper = new ResourceHelper(serverPort);
  }

  @Test
  public void testSkippableFilterExclusions() {
    Response response = resourceHelper.executeGet("/status");
    assertNull(response.getHeaderString("x-passed-filter"));
  }

  @Test
  public void testSkippableFilterNoExclusions() {
    Response response = resourceHelper.executeGet("/status-not");
    assertEquals("true", response.getHeaderString("x-passed-filter"));
  }

  public static class AddHeaderSkippableFilter extends SkippableFilter {
    public AddHeaderSkippableFilter() {
    }

    @Override
    protected void performFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      response.addHeader("x-passed-filter", "true");
      filterChain.doFilter(request, response);
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(NabTestConfig.class)
  public static class FilterApplicationOverride {

    @Bean
    public FilterRegistrationBean<AddHeaderSkippableFilter> addHeaderSkippableFilter() {
      FilterRegistrationBean<AddHeaderSkippableFilter> registration = new FilterRegistrationBean<>(new AddHeaderSkippableFilter());
      registration.addInitParameter("exclusionsString", "/status");
      return registration;
    }
  }
}
