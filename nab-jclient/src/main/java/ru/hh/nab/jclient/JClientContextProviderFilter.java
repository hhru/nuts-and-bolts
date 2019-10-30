package ru.hh.nab.jclient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.nab.common.component.NabServletFilter;
import ru.hh.nab.common.servlet.UriComponent;
import static java.util.Objects.requireNonNull;
import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.NONNULL;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class JClientContextProviderFilter implements Filter, NabServletFilter {
  private final HttpClientContextThreadLocalSupplier contextThreadLocalSupplier;

  public JClientContextProviderFilter(HttpClientContextThreadLocalSupplier contextThreadLocalSupplier) {
    this.contextThreadLocalSupplier = contextThreadLocalSupplier;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    requireNonNull(contextThreadLocalSupplier, "httpClientContextSupplier should not be null");
    try {
      contextThreadLocalSupplier.addContext(getRequestHeadersMap(request), getQueryParamsMap(request));
      chain.doFilter(request, response);
    } finally {
      contextThreadLocalSupplier.clear();
    }
  }

  private static Map<String, List<String>> getRequestHeadersMap(ServletRequest req) {
    HttpServletRequest request = (HttpServletRequest) req;
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(request.getHeaderNames().asIterator(), DISTINCT | NONNULL), false)
      .collect(toMap(identity(), h -> List.of(request.getHeader(h))));
  }

  private static Map<String, List<String>> getQueryParamsMap(ServletRequest req) {
    HttpServletRequest request = (HttpServletRequest) req;
    return UriComponent.decodeQuery(request.getQueryString(), true, true);
  }
}
