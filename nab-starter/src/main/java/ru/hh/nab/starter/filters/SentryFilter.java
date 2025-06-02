package ru.hh.nab.starter.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.hh.nab.starter.sentry.SentryScopeConfigurator;
import ru.hh.nab.starter.server.RequestHeaders;

public class SentryFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    try {
      // TODO: https://jira.hh.ru/browse/HH-233805
      String requestId = request.getHeader(RequestHeaders.REQUEST_ID);
      if (requestId != null) {
        SentryScopeConfigurator.setTraceId(requestId);
      }
      filterChain.doFilter(request, response);
    } finally {
      SentryScopeConfigurator.clearTraceId();
    }
  }
}
