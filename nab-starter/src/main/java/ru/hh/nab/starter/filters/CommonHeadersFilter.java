package ru.hh.nab.starter.filters;

import org.springframework.web.filter.OncePerRequestFilter;
import ru.hh.nab.starter.http.RequestContext;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class CommonHeadersFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    String source = request.getHeader(RequestHeaders.REQUEST_SOURCE);
    boolean isLoadTesting = request.getHeader(RequestHeaders.LOAD_TESTING) != null;

    try {
      RequestContext.setRequestSource(source);
      RequestContext.setLoadTesting(isLoadTesting);

      filterChain.doFilter(request, response);

    } finally {
      RequestContext.clearRequestSource();
      RequestContext.clearLoadTesting();
    }
  }
}
