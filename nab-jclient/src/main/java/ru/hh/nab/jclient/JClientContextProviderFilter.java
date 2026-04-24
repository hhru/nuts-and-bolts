package ru.hh.nab.jclient;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.NONNULL;
import java.util.Spliterators;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import java.util.stream.StreamSupport;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.nab.common.servlet.UriComponent;

public class JClientContextProviderFilter implements Filter {
  private final HttpClientContextThreadLocalSupplier contextThreadLocalSupplier;

  public JClientContextProviderFilter(HttpClientContextThreadLocalSupplier contextThreadLocalSupplier) {
    this.contextThreadLocalSupplier = contextThreadLocalSupplier;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    requireNonNull(contextThreadLocalSupplier, "httpClientContextSupplier should not be null");
    try {
      try {
        contextThreadLocalSupplier.addContext(getRequestHeadersMap(request), getQueryParamsMap(request));
      } catch (IllegalArgumentException e) {
        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      long timeLeft = contextThreadLocalSupplier.get().getDeadlineContext().getTimeLeft();

      if (!request.isAsyncStarted()) {
        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(timeLeft);
        asyncContext.start(() -> {
          try {
            chain.doFilter(request, response);
          } catch (Exception e) {
            ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          } finally {
            asyncContext.complete();
          }
        });
      } else {
        request.getAsyncContext().setTimeout(timeLeft);
        chain.doFilter(request, response);
      }
    } finally {
      contextThreadLocalSupplier.clear();
    }
  }

  private static Map<String, List<String>> getRequestHeadersMap(ServletRequest req) {
    HttpServletRequest request = (HttpServletRequest) req;
    return StreamSupport
        .stream(Spliterators.spliteratorUnknownSize(request.getHeaderNames().asIterator(), DISTINCT | NONNULL), false)
        .collect(toMap(identity(), h -> List.of(request.getHeader(h))));
  }

  private static Map<String, List<String>> getQueryParamsMap(ServletRequest req) {
    HttpServletRequest request = (HttpServletRequest) req;
    return UriComponent.decodeQuery(request.getQueryString(), true, true);
  }
}
