package ru.hh.nab.sentry;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import ru.hh.nab.common.constants.RequestHeaders;

public class SentryFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    try {
      // TODO: https://jira.hh.ru/browse/HH-233805
      String requestId = httpRequest.getHeader(RequestHeaders.REQUEST_ID);
      if (requestId != null) {
        SentryScopeConfigurator.setTraceId(requestId);
      }
      chain.doFilter(request, response);
    } finally {
      SentryScopeConfigurator.clearTraceId();
    }
  }
}
