package ru.hh.nab.jclient;

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.IOException;
import java.util.Collections;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;

public class JClientContextProviderFilterTest {

  @Test
  public void testInvalidQueryParams() throws IOException, ServletException {
    OpenTelemetrySdk telemetrySdk = OpenTelemetrySdk.builder()
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance())).build();

    JClientContextProviderFilter filter = new JClientContextProviderFilter(mock(HttpClientContextThreadLocalSupplier.class),
        telemetrySdk.getTracer("test"), new TelemetryPropagator(telemetrySdk));
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(request.getQueryString()).thenReturn("/query?%");

    filter.doFilter(request, response, chain);
    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
    verify(chain, never()).doFilter(any(), any());
  }
}
