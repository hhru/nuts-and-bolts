package ru.hh.nab.starter.filters;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.JettyStarterTestBase;

@ContextConfiguration(classes = {NabTestConfig.class})
public class RequestIdLoggingFilterTest extends JettyStarterTestBase {

  @Test
  public void testRequestId() throws Exception {
    String testRequestId = "123";

    RequestBuilder requestBuilder = RequestBuilder.get("/status");
    requestBuilder.addHeader(RequestHeaders.REQUEST_ID, testRequestId);

    HttpResponse response = httpClient().execute(requestBuilder.build());

    assertEquals(testRequestId, response.getFirstHeader(RequestHeaders.REQUEST_ID).getValue());
  }

  @Test
  public void testNoRequestId() throws Exception {
    HttpResponse response = httpClient().execute(RequestBuilder.get("/status").build());

    assertNull(response.getFirstHeader(RequestHeaders.REQUEST_ID));
  }
}
