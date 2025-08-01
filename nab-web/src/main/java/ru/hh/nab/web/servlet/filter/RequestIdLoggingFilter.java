package ru.hh.nab.web.servlet.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.hh.nab.common.constants.RequestHeaders;
import ru.hh.nab.common.mdc.MDC;

public final class RequestIdLoggingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

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
