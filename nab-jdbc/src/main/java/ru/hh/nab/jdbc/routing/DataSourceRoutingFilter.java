package ru.hh.nab.jdbc.routing;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import org.glassfish.jersey.uri.UriComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.jdbc.common.DataSourceType;

public class DataSourceRoutingFilter implements Filter {
  private static final Logger LOG = LoggerFactory.getLogger(DataSourceRoutingFilter.class);

  static final String REPLICA_ONLY_RQ = "replicaOnlyRq";
  static final String NAB_TARGET_DATA_SOURCE = "nabTargetDataSource";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    try {
      HttpServletRequest httpServletRequest = (HttpServletRequest) request;
      MultivaluedMap<String, String> queryParams = UriComponent.decodeQuery(httpServletRequest.getQueryString(), false);
      String targetDataSource = queryParams.getFirst(NAB_TARGET_DATA_SOURCE);
      if (targetDataSource != null && !targetDataSource.isEmpty()) {
        wrapInDataSource(request, response, chain, targetDataSource);
      } else if (Boolean.parseBoolean(queryParams.getFirst(REPLICA_ONLY_RQ))) {
        LOG.debug(REPLICA_ONLY_RQ + " used. It's deprecated, use " + NAB_TARGET_DATA_SOURCE + " parameter");
        wrapInDataSource(request, response, chain, DataSourceType.READONLY);
      } else {
        DataSourceContextUnsafe.setDefaultMDC();
        chain.doFilter(request, response);
      }
    } finally {
      DataSourceContextUnsafe.clearMDC();
    }
  }

  protected void wrapInDataSource(ServletRequest request, ServletResponse response, FilterChain chain, String targetDataSource) {
    DataSourceContextUnsafe.executeInScope(targetDataSource, () -> DataSourceContext.onDataSource(targetDataSource, () -> {
      try {
        chain.doFilter(request, response);
      } catch (IOException | ServletException e) {
        throw new RuntimeException(e);
      }
      return null;
    }));
  }
}
