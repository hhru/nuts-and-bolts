package ru.hh.nab.web.servlet.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import static java.util.Optional.ofNullable;
import org.springframework.web.filter.OncePerRequestFilter;
import static ru.hh.jclient.common.HttpHeaderNames.X_OUTER_TIMEOUT_MS;
import ru.hh.nab.common.constants.RequestHeaders;
import ru.hh.nab.web.http.RequestContext;

public final class CommonHeadersFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    var source = request.getHeader(RequestHeaders.REQUEST_SOURCE);
    var isLoadTesting = request.getHeader(RequestHeaders.LOAD_TESTING) != null;
    var outerTimeoutMs = request.getHeader(X_OUTER_TIMEOUT_MS);

    try {
      RequestContext.setRequestSource(source);
      RequestContext.setLoadTesting(isLoadTesting);
      RequestContext.setOuterTimeoutMs(ofNullable(outerTimeoutMs).map(Long::valueOf).orElse(null));

      filterChain.doFilter(request, response);

    } finally {
      RequestContext.clearLoadTesting();
      RequestContext.clearRequestSource();
      RequestContext.clearOuterTimeout();
    }
  }
}
