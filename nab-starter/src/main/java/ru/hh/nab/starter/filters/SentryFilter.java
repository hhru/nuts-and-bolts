package ru.hh.nab.starter.filters;

import io.sentry.HubAdapter;
import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.hh.nab.starter.server.RequestHeaders;

public class SentryFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SentryFilter.class);

  @SuppressWarnings("UnstableApiUsage")
  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String requestId = request.getHeader(RequestHeaders.REQUEST_ID);
    if (HubAdapter.getInstance().isEnabled() && requestId != null) {
      Sentry.configureScope(scope -> {
        try {
          scope.getPropagationContext().setTraceId(new SentryId(requestId));
        } catch (RuntimeException e) {
          LOGGER.warn("Unable to set sentry trace id: {}", requestId, e);
        }
      });
    }
    filterChain.doFilter(request, response);
  }
}
