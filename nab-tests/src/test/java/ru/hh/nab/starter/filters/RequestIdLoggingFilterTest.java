package ru.hh.nab.starter.filters;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.starter.server.RequestHeaders;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;

import javax.ws.rs.core.Response;

@ContextConfiguration(classes = {NabTestConfig.class})
public class RequestIdLoggingFilterTest extends NabTestBase {

  @Test
  public void testRequestId() {
    final String testRequestId = "123";

    Response response = createRequest("/status")
        .header(RequestHeaders.REQUEST_ID_HEADER, testRequestId)
        .get();

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals(testRequestId, response.getHeaderString(RequestHeaders.REQUEST_ID_HEADER));
  }

  @Test
  public void testNoRequestId() {
    Response response = executeGet("/status");

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertNull(response.getHeaderString(RequestHeaders.REQUEST_ID_HEADER));
  }
}
