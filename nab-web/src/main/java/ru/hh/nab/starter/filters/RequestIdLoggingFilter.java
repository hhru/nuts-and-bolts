package ru.hh.nab.starter.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.hh.nab.common.mdc.MDC;
import ru.hh.nab.common.servlet.SystemFilter;
import ru.hh.nab.starter.server.RequestHeaders;

public final class RequestIdLoggingFilter extends OncePerRequestFilter implements SystemFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    String requestId = request.getHeader(RequestHeaders.REQUEST_ID);
    try {
      if (requestId == null) {
        requestId = RequestHeaders.EMPTY_REQUEST_ID;
      } else {
        response.addHeader(RequestHeaders.REQUEST_ID, requestId);
      }
      MDC.setRequestId(requestId);

      filterChain.doFilter(request, response);

    } finally {
      MDC.clearRequestId();
    }
  }
}
