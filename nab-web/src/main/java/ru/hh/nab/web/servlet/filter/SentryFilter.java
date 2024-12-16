package ru.hh.nab.web.servlet.filter;

import io.sentry.HubAdapter;
import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.hh.nab.web.http.RequestHeaders;

public class SentryFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SentryFilter.class);

  @SuppressWarnings("UnstableApiUsage")
  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    // TODO: https://jira.hh.ru/browse/HH-233805
    String requestId = request.getHeader(RequestHeaders.REQUEST_ID);
    if (HubAdapter.getInstance().isEnabled() && requestId != null) {
      Sentry.configureScope(scope -> {
        try {
          scope.getPropagationContext().setTraceId(new SentryId(requestId));
        } catch (RuntimeException e) {
          // TODO: it's better to use warn/error log level, but there are too much invalid rids.
          //  Fix log level to warn/error after https://jira.hh.ru/browse/PORTFOLIO-19764
          LOGGER.debug("Unable to set sentry trace id: {}", requestId, e);
        }
      });
    }
    filterChain.doFilter(request, response);
  }
}
