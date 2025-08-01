package ru.hh.nab.web.servlet.filter;

import jakarta.inject.Inject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import ru.hh.nab.web.NabWebTestConfig;

@SpringBootTest(classes = SkippableFilterTest.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SkippableFilterTest {

  @Inject
  private TestRestTemplate testRestTemplate;

  @Test
  public void testSkippableFilterExclusions() {
    ResponseEntity<String> response = testRestTemplate.getForEntity("/status", String.class);
    assertNull(response.getHeaders().get("x-passed-filter"));
  }

  @Test
  public void testSkippableFilterNoExclusions() {
    ResponseEntity<String> response = testRestTemplate.getForEntity("/status-not", String.class);
    assertEquals(List.of("true"), response.getHeaders().get("x-passed-filter"));
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
  @Import(NabWebTestConfig.class)
  public static class TestConfiguration {

    @Bean
    public FilterRegistrationBean<AddHeaderSkippableFilter> addHeaderSkippableFilter() {
      FilterRegistrationBean<AddHeaderSkippableFilter> registration = new FilterRegistrationBean<>(new AddHeaderSkippableFilter());
      registration.addInitParameter("exclusionsString", "/status");
      return registration;
    }
  }
}
