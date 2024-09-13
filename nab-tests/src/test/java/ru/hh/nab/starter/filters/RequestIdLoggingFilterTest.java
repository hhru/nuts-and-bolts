package ru.hh.nab.starter.filters;

import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.starter.server.RequestHeaders;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RequestIdLoggingFilterTest {

  private final ResourceHelper resourceHelper;

  public RequestIdLoggingFilterTest(@LocalServerPort int serverPort) {
    this.resourceHelper = new ResourceHelper(serverPort);
  }

  @Test
  public void testRequestId() {
    final String testRequestId = "123";

    Response response = resourceHelper
        .createRequest("/status")
        .header(RequestHeaders.REQUEST_ID, testRequestId)
        .get();

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals(testRequestId, response.getHeaderString(RequestHeaders.REQUEST_ID));
  }

  @Test
  public void testNoRequestId() {
    Response response = resourceHelper.executeGet("/status");

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertNull(response.getHeaderString(RequestHeaders.REQUEST_ID));
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(NabTestConfig.class)
  public static class TestConfiguration {
  }
}
