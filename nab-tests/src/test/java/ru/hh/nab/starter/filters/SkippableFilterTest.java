package ru.hh.nab.starter.filters;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.starter.servlet.DefaultServletConfig;
import ru.hh.nab.starter.servlet.ServletConfig;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.EnumSet;

@ContextConfiguration(classes = {NabTestConfig.class})
public class SkippableFilterTest extends NabTestBase {

  public static class AddHeaderSkippableFilter extends SkippableFilter {
    public AddHeaderSkippableFilter() {}

    @Override
    protected void performFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      response.addHeader("x-passed-filter", "true");
      filterChain.doFilter(request, response);
    }
  }

  @Override
  protected ServletConfig getServletConfig() {
    return new DefaultServletConfig() {
      @Override
      public void configureServletContext(ServletContextHandler servletContextHandler, ApplicationContext applicationContext) {
        FilterHolder holder = new FilterHolder(AddHeaderSkippableFilter.class);
        holder.setInitParameter("exclusionsString", "/status");
        servletContextHandler.addFilter(holder, "/*", EnumSet.allOf(DispatcherType.class));
      }
    };
  }

  @Test
  public void testSkippableFilterExclusions() {
    Response response = executeGet("/status");

    assertNull(response.getHeaderString("x-passed-filter"));
  }

  @Test
  public void testSkippableFilterNoExclusions() {
    Response response = executeGet("/status-not");

    assertEquals(response.getHeaderString("x-passed-filter"), "true");
  }
}
