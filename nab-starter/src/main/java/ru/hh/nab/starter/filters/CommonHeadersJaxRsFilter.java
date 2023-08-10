package ru.hh.nab.starter.filters;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import static java.util.Optional.ofNullable;
import static ru.hh.jclient.common.HttpHeaderNames.X_OUTER_TIMEOUT_MS;
import ru.hh.nab.starter.http.RequestContext;
import ru.hh.nab.starter.server.RequestHeaders;

@Provider
@ApplicationScoped
public final class CommonHeadersJaxRsFilter implements ContainerRequestFilter, ContainerResponseFilter {
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    var source = requestContext.getHeaderString(RequestHeaders.REQUEST_SOURCE);
    var isLoadTesting = requestContext.getHeaderString(RequestHeaders.LOAD_TESTING) != null;
    var outerTimeoutMs = requestContext.getHeaderString(X_OUTER_TIMEOUT_MS);

    RequestContext.setRequestSource(source);
    RequestContext.setLoadTesting(isLoadTesting);
    RequestContext.setOuterTimeoutMs(ofNullable(outerTimeoutMs).map(Long::valueOf).orElse(null));
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    RequestContext.clearLoadTesting();
    RequestContext.clearRequestSource();
    RequestContext.clearOuterTimeout();
  }
}
