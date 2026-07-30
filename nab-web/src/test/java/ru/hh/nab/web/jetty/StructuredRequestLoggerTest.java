package ru.hh.nab.web.jetty;

import com.timgroup.statsd.StatsDClient;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.ConnectionMetaData;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.NanoTime;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import ru.hh.metrics.StatsDSender;
import static ru.hh.nab.common.constants.RequestAttributes.HTTP_ROUTE;
import static ru.hh.nab.web.http.RequestInfo.CACHE_ATTRIBUTE;
import static ru.hh.nab.web.http.RequestInfo.HIT;

public class StructuredRequestLoggerTest {

  private static final String SERVICE_NAME = "test-service";

  @Test
  public void testRequestTimeMetricIsSentWithTags() {
    StatsDClient statsDClient = mock(StatsDClient.class);
    StatsDSender statsDSender = new StatsDSender(statsDClient, mock(ScheduledExecutorService.class));

    StructuredRequestLogger logger = new StructuredRequestLogger(statsDSender, SERVICE_NAME);
    logger.log(createRequest(), createResponse());

    ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
    verify(statsDClient).time(
        eq("service_requests_time.app_is_%s.method_is_GET.httpRoute_is_/vacancies/{id}.status_is_200.cache_status_is_HIT".formatted(SERVICE_NAME)),
        timeCaptor.capture()
    );
    assertTrue(timeCaptor.getValue() >= 0);
  }

  @Test
  public void testNoMetricsWithoutStatsDSender() {
    StructuredRequestLogger logger = new StructuredRequestLogger();
    logger.log(createRequest(), createResponse());
  }

  private static Request createRequest() {
    Request request = mock(Request.class);
    when(request.getHeaders()).thenReturn(HttpFields.EMPTY);
    when(request.getConnectionMetaData()).thenReturn(mock(ConnectionMetaData.class));
    when(request.getAttribute(CACHE_ATTRIBUTE)).thenReturn(HIT);
    when(request.getAttribute(HTTP_ROUTE)).thenReturn("/vacancies/{id}");
    when(request.getMethod()).thenReturn("GET");
    when(request.getHttpURI()).thenReturn(HttpURI.from("/vacancies/123"));
    when(request.getHeadersNanoTime()).thenReturn(NanoTime.now());
    return request;
  }

  private static Response createResponse() {
    Response response = mock(Response.class);
    when(response.getHeaders()).thenReturn(HttpFields.build());
    when(response.getStatus()).thenReturn(200);
    return response;
  }
}
