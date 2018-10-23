package ru.hh.nab.starter.filters;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import org.eclipse.jetty.servlet.FilterHolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.starter.NabServletContextConfig;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;

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
  protected NabServletContextConfig getServletConfig() {
    return new NabServletContextConfig() {

      @Override
      protected void configureServletContext(ServletContext servletContext, WebApplicationContext rootCtx) {
        FilterHolder holder = new FilterHolder(AddHeaderSkippableFilter.class);
        holder.setInitParameter("exclusionsString", "/status");
        registerFilter(servletContext, holder.getName(), holder, EnumSet.allOf(DispatcherType.class), DEFAULT_MAPPING);
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
