package ru.hh.nab.hibernate.routing;

import java.io.IOException;
import java.util.function.Supplier;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import ru.hh.nab.datasource.DataSourceType;
import ru.hh.nab.hibernate.transaction.DataSourceContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DataSourceContext.class)
public class DataSourceRoutingFilterTest {

  @Test
  public void testNabTargetDataSourceWorks() throws IOException, ServletException {
    PowerMockito.spy(DataSourceContext.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    when(request.getQueryString()).thenReturn(DataSourceRoutingFilter.NAB_TARGET_DATA_SOURCE + "=test");
    FilterChain chain = mock(FilterChain.class);
    DataSourceRoutingFilter filter = new DataSourceRoutingFilter();
    filter.doFilter(request, response, chain);

    verifyStatic(DataSourceContext.class);
    DataSourceContext.onDataSource(eq("test"), any(Supplier.class));
  }

  @Test
  public void testNabTargetDataSourceWorksOnlyWithUrl() throws IOException, ServletException {
    PowerMockito.spy(DataSourceContext.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    when(request.getParameter(DataSourceRoutingFilter.NAB_TARGET_DATA_SOURCE)).thenReturn("test");
    FilterChain chain = mock(FilterChain.class);
    DataSourceRoutingFilter filter = new DataSourceRoutingFilter();
    filter.doFilter(request, response, chain);

    verifyStatic(DataSourceContext.class, never());
    DataSourceContext.onDataSource(eq("test"), any(Supplier.class));
  }

  @Test
  public void testReplicaOnlyRqWorks() throws IOException, ServletException {
    PowerMockito.spy(DataSourceContext.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    when(request.getQueryString()).thenReturn(DataSourceRoutingFilter.REPLICA_ONLY_RQ + "=true");
    FilterChain chain = mock(FilterChain.class);
    DataSourceRoutingFilter filter = new DataSourceRoutingFilter();
    filter.doFilter(request, response, chain);

    verifyStatic(DataSourceContext.class);
    DataSourceContext.onDataSource(eq(DataSourceType.READONLY), any(Supplier.class));
  }

  @Test
  public void testNabTargetDataSourceHasPriority() throws IOException, ServletException {
    PowerMockito.spy(DataSourceContext.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    when(request.getQueryString()).thenReturn(DataSourceRoutingFilter.REPLICA_ONLY_RQ + "=true");
    when(request.getQueryString()).thenReturn(DataSourceRoutingFilter.NAB_TARGET_DATA_SOURCE + "=test");
    FilterChain chain = mock(FilterChain.class);
    DataSourceRoutingFilter filter = new DataSourceRoutingFilter();
    filter.doFilter(request, response, chain);

    verifyStatic(DataSourceContext.class);
    DataSourceContext.onDataSource(eq("test"), any(Supplier.class));
  }

  @Test
  public void testNoParametersPresent() throws IOException, ServletException {
    PowerMockito.spy(DataSourceContext.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    DataSourceRoutingFilter filter = new DataSourceRoutingFilter();
    filter.doFilter(request, response, chain);

    verifyStatic(DataSourceContext.class, never());
    DataSourceContext.onDataSource(anyString(), any(Supplier.class));
  }

}
