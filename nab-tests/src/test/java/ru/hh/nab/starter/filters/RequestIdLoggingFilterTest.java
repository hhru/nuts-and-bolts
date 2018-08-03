package ru.hh.nab.starter.filters;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import ru.hh.nab.testbase.NabJerseyTestBase;

import javax.ws.rs.core.Response;

public class RequestIdLoggingFilterTest extends NabJerseyTestBase {

  @Test
  public void testRequestId() {
    final String testRequestId = "123";

    Response response = createRequest("status")
        .header(RequestHeaders.REQUEST_ID, testRequestId)
        .get();

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals(testRequestId, response.getHeaderString(RequestHeaders.REQUEST_ID));
  }

  @Test
  public void testNoRequestId() {
    Response response = executeGet("status");

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertNull(response.getHeaderString(RequestHeaders.REQUEST_ID));
  }
}
