package ru.hh.nab.jclient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
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

@Provider
@ApplicationScoped
public class JClientContextProvider implements ContainerRequestFilter, ContainerResponseFilter {

  private final HttpClientContextThreadLocalSupplier contextThreadLocalSupplier;

  @Inject
  public JClientContextProvider(HttpClientContextThreadLocalSupplier contextThreadLocalSupplier) {
    this.contextThreadLocalSupplier = contextThreadLocalSupplier;
  }

  private static Map<String, List<String>> getRequestHeadersMap(ContainerRequestContext context) {
    return StreamSupport
        .stream(Spliterators.spliteratorUnknownSize(context.getHeaders().keySet().iterator(), DISTINCT | NONNULL), false)
        .collect(toMap(identity(), h -> List.of(context.getHeaderString(h))));
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) {
    requireNonNull(contextThreadLocalSupplier, "httpClientContextSupplier should not be null");
    try {
      contextThreadLocalSupplier.addContext(getRequestHeadersMap(containerRequestContext),
          containerRequestContext.getUriInfo().getQueryParameters(true)
      );
    } catch (IllegalArgumentException e) {
      contextThreadLocalSupplier.clear();
      throw e;
    }
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    contextThreadLocalSupplier.clear();
  }
}
