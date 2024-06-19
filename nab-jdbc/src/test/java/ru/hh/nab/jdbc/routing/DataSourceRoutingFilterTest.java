package ru.hh.nab.jdbc.routing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import ru.hh.nab.jdbc.common.DataSourceType;

public class DataSourceRoutingFilterTest {

  @Test
  public void testNabTargetDataSourceWorks() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    when(request.getQueryString()).thenReturn(DataSourceRoutingFilter.NAB_TARGET_DATA_SOURCE + "=test");
    FilterChain chain = mock(FilterChain.class);
    DataSourceRoutingFilter filter = spy(new DataSourceRoutingFilter());
    filter.doFilter(request, response, chain);
    verify(filter).wrapInDataSource(request, response, chain, "test");
  }

  @Test
  public void testNabTargetDataSourceWorksOnlyWithUrl() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    when(request.getParameter(DataSourceRoutingFilter.NAB_TARGET_DATA_SOURCE)).thenReturn("test");
    FilterChain chain = mock(FilterChain.class);
    DataSourceRoutingFilter filter = spy(new DataSourceRoutingFilter());
    filter.doFilter(request, response, chain);
    verify(filter, never()).wrapInDataSource(request, response, chain, "test");
  }

  @Test
  public void testReplicaOnlyRqWorks() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    when(request.getQueryString()).thenReturn(DataSourceRoutingFilter.REPLICA_ONLY_RQ + "=true");
    FilterChain chain = mock(FilterChain.class);
    DataSourceRoutingFilter filter = spy(new DataSourceRoutingFilter());
    filter.doFilter(request, response, chain);
    verify(filter).wrapInDataSource(request, response, chain, DataSourceType.READONLY);
  }

  @Test
  public void testNabTargetDataSourceHasPriority() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    when(request.getQueryString()).thenReturn(DataSourceRoutingFilter.REPLICA_ONLY_RQ + "=true");
    when(request.getQueryString()).thenReturn(DataSourceRoutingFilter.NAB_TARGET_DATA_SOURCE + "=test");
    FilterChain chain = mock(FilterChain.class);
    DataSourceRoutingFilter filter = spy(new DataSourceRoutingFilter());
    filter.doFilter(request, response, chain);
    verify(filter).wrapInDataSource(request, response, chain, "test");
  }

  @Test
  public void testNoParametersPresent() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    DataSourceRoutingFilter filter = spy(new DataSourceRoutingFilter());
    filter.doFilter(request, response, chain);
    verify(filter, never()).wrapInDataSource(request, response, chain, "");
  }
}
