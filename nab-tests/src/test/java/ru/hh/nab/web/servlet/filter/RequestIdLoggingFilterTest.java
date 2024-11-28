package ru.hh.nab.web.servlet.filter;

import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.hh.nab.testbase.web.WebTestBase;
import ru.hh.nab.web.NabWebTestConfig;
import ru.hh.nab.web.http.RequestHeaders;

@SpringBootTest(classes = NabWebTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RequestIdLoggingFilterTest extends WebTestBase {

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
}
