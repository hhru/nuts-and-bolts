package ru.hh.nab.web.servlet.filter;

import jakarta.inject.Inject;
import static jakarta.ws.rs.core.Response.Status.OK;
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
import static org.springframework.http.RequestEntity.get;
import org.springframework.http.ResponseEntity;
import ru.hh.nab.common.constants.RequestHeaders;
import ru.hh.nab.web.NabWebTestConfig;

@SpringBootTest(classes = RequestIdLoggingFilterTest.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RequestIdLoggingFilterTest {

  @Inject
  private TestRestTemplate testRestTemplate;

  @Test
  public void testRequestId() {
    final String testRequestId = "123";

    ResponseEntity<String> response = testRestTemplate.exchange(
        get("/status").header(RequestHeaders.REQUEST_ID, testRequestId).build(),
        String.class
    );

    assertEquals(OK.getStatusCode(), response.getStatusCode().value());
    assertEquals(List.of(testRequestId), response.getHeaders().get(RequestHeaders.REQUEST_ID));
  }

  @Test
  public void testNoRequestId() {
    ResponseEntity<String> response = testRestTemplate.getForEntity("/status", String.class);

    assertEquals(OK.getStatusCode(), response.getStatusCode().value());
    assertNull(response.getHeaders().get(RequestHeaders.REQUEST_ID));
  }

  @Configuration
  @Import(NabWebTestConfig.class)
  public static class TestConfiguration {

    @Bean
    public FilterRegistrationBean<RequestIdLoggingFilter> requestIdLoggingFilter() {
      return new FilterRegistrationBean<>(new RequestIdLoggingFilter());
    }
  }
}
