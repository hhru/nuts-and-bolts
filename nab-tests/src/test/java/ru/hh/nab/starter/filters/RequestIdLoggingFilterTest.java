package ru.hh.nab.starter.filters;

import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import ru.hh.nab.starter.server.RequestHeaders;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.testbase.extensions.HHJetty;
import ru.hh.nab.testbase.extensions.HHJettyExtension;

@ExtendWith({
    HHJettyExtension.class,
})
@SpringJUnitWebConfig({
    NabTestConfig.class
})
public class RequestIdLoggingFilterTest {
  @HHJetty
  ResourceHelper resourceHelper;

  @Test
  public void testRequestId() {
    final String testRequestId = "123";

    Response response = resourceHelper.createRequest("/status")
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
