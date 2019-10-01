package ru.hh.nab.starter.filters;

import org.springframework.web.filter.OncePerRequestFilter;
import ru.hh.nab.starter.http.RequestContext;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import ru.hh.nab.starter.server.RequestHeaders;
import static java.util.Optional.ofNullable;

public final class CommonHeadersFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    var source = request.getHeader(RequestHeaders.REQUEST_SOURCE);
    var isLoadTesting = request.getHeader(RequestHeaders.LOAD_TESTING) != null;
    var outerTimeoutMs = request.getHeader(RequestHeaders.OUTER_TIMEOUT_MS);

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
