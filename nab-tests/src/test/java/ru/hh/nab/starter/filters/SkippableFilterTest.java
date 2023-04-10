package ru.hh.nab.starter.filters;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.testbase.extensions.NabJunitWebConfig;
import ru.hh.nab.testbase.extensions.NabTestServer;
import ru.hh.nab.testbase.extensions.OverrideNabApplication;

@NabJunitWebConfig(NabTestConfig.class)
public class SkippableFilterTest {

  @NabTestServer(overrideApplication = FilterApplicationOverride.class)
  ResourceHelper resourceHelper;

  @Test
  public void testSkippableFilterExclusions() {
    Response response = resourceHelper.executeGet("/status");
    assertNull(response.getHeaderString("x-passed-filter"));
  }

  @Test
  public void testSkippableFilterNoExclusions() {
    Response response = resourceHelper.executeGet("/status-not");
    assertEquals("true", response.getHeaderString("x-passed-filter"));
  }

  public static class AddHeaderSkippableFilter extends SkippableFilter {
    public AddHeaderSkippableFilter() {
    }

    @Override
    protected void performFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      response.addHeader("x-passed-filter", "true");
      filterChain.doFilter(request, response);
    }
  }

  public static class FilterApplicationOverride implements OverrideNabApplication {
    @Override
    public NabApplication getNabApplication() {
      return NabApplication.builder().addFilter(AddHeaderSkippableFilter.class).addInitParameter("exclusionsString", "/status").bindToRoot().build();
    }
  }
}
