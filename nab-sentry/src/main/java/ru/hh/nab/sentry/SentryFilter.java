package ru.hh.nab.sentry;

import io.sentry.HubAdapter;
import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.constants.RequestHeaders;

public class SentryFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SentryFilter.class);

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    // TODO: https://jira.hh.ru/browse/HH-233805
    String requestId = httpRequest.getHeader(RequestHeaders.REQUEST_ID);
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
    chain.doFilter(request, response);
  }
}
