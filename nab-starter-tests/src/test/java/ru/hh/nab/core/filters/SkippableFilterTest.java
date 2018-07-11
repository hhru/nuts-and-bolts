package ru.hh.nab.core.filters;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.core.CoreTestConfig;
import ru.hh.nab.core.servlet.DefaultServletConfig;
import ru.hh.nab.core.servlet.ServletConfig;
import ru.hh.nab.testbase.JettyLauncherTestBase;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@ContextConfiguration(classes = {CoreTestConfig.class})
public class SkippableFilterTest extends JettyLauncherTestBase {

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
  protected ServletConfig servletConfig() {
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
  public void testSkippableFilterExclusions() throws IOException {
    HttpResponse response = httpClient().execute(RequestBuilder.get("/status").build());

    assertNull(response.getFirstHeader("x-passed-filter"));
  }

  @Test
  public void testSkippableFilterNoExclusions() throws IOException {
    HttpResponse response = httpClient().execute(RequestBuilder.get("/status-not").build());

    assertEquals(response.getFirstHeader("x-passed-filter").getValue(), "true");
  }
}
