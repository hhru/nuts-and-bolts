package ru.hh.nab.web.jetty;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.ConnectionMetaData;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import ru.hh.metrics.MetricsSender;
import ru.hh.metrics.Tag;
import static ru.hh.nab.common.constants.RequestAttributes.HTTP_ROUTE;
import static ru.hh.nab.web.http.RequestInfo.CACHE_ATTRIBUTE;
import static ru.hh.nab.web.http.RequestInfo.HIT;

public class StructuredRequestLoggerTest {

  private static final String SERVICE_NAME = "test-service";

  @Test
  public void testRequestTimeMetricIsSentWithTags() {
    MetricsSender metricsSender = mock(MetricsSender.class);

    StructuredRequestLogger logger = new StructuredRequestLogger(metricsSender, SERVICE_NAME);
    logger.log(createRequest(), createResponse());

    ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
    verify(metricsSender).sendTime(
        eq("service.request.time"),
        timeCaptor.capture(),
        eq(new Tag("app", SERVICE_NAME)),
        eq(new Tag("method", "GET")),
        eq(new Tag("http_route", "/vacancies/{id}")),
        eq(new Tag("status", "200")),
        eq(new Tag("cache_status", "HIT"))
    );
    assertTrue(timeCaptor.getValue() >= 0);
  }

  private static Request createRequest() {
    Request request = mock(Request.class);
    when(request.getHeaders()).thenReturn(HttpFields.EMPTY);
    when(request.getConnectionMetaData()).thenReturn(mock(ConnectionMetaData.class));
    when(request.getAttribute(CACHE_ATTRIBUTE)).thenReturn(HIT);
    when(request.getAttribute(HTTP_ROUTE)).thenReturn("/vacancies/{id}");
    when(request.getMethod()).thenReturn("GET");
    return request;
  }

  private static Response createResponse() {
    Response response = mock(Response.class);
    when(response.getHeaders()).thenReturn(HttpFields.build());
    when(response.getStatus()).thenReturn(200);
    return response;
  }
}
