package ru.hh.nab.hibernate.routing;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.datasource.DataSourceType;
import ru.hh.nab.hibernate.transaction.DataSourceContext;
import ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe;

public class DataSourceRoutingFilter implements Filter {
  private static final Logger LOG = LoggerFactory.getLogger(DataSourceRoutingFilter.class);

  static final String REPLICA_ONLY_RQ = "replicaOnlyRq";
  static final String NAB_TARGET_DATA_SOURCE = "nabTargetDataSource";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    try {
      String targetDataSource = request.getParameter(NAB_TARGET_DATA_SOURCE);
      if (targetDataSource != null && !targetDataSource.isEmpty()) {
        wrapInDataSource(request, response, chain, targetDataSource);
      } else if (Boolean.parseBoolean(request.getParameter(REPLICA_ONLY_RQ))) {
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
    try {
      DataSourceContextUnsafe.setRequestScopeDataSourceKey(targetDataSource);
      DataSourceContext.executeOn(targetDataSource, () -> {
        try {
          chain.doFilter(request, response);
        } catch (IOException | ServletException e) {
          throw new RuntimeException(e);
        }
        return null;
      });
    } finally {
      DataSourceContextUnsafe.clearRequestScopeDataSourceKey();
    }
  }

  @Override
  public void destroy() {
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }
}
