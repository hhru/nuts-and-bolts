package ru.hh.nab.starter.filters;

import jakarta.annotation.Priority;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.hh.nab.common.component.NabServletFilter;
import ru.hh.nab.common.servlet.ServletFilterPriorities;
import ru.hh.nab.starter.sentry.SentryScopeConfigurator;
import ru.hh.nab.starter.server.RequestHeaders;

@Priority(ServletFilterPriorities.OBSERVABILITY)
public class SentryFilter extends OncePerRequestFilter implements NabServletFilter {

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
