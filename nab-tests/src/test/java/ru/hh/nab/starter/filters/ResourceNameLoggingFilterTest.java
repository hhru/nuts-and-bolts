package ru.hh.nab.starter.filters;

import static javax.ws.rs.core.Response.Status.OK;
import org.glassfish.jersey.server.ResourceConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.common.mdc.MDC;
import ru.hh.nab.starter.servlet.DefaultServletConfig;
import ru.hh.nab.starter.servlet.ServletConfig;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@ContextConfiguration(classes = {NabTestConfig.class})
public class ResourceNameLoggingFilterTest extends NabTestBase {

  @Override
  protected ServletConfig getServletConfig() {
    return new DefaultServletConfig() {
      @Override
      public void setupResourceConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(TestResource.class);
      }
    };
  }

  @Test
  public void testResourceName() {
    assertFalse(MDC.getController().isPresent());

    Response response = executeGet("/test");

    assertEquals(OK.getStatusCode(), response.getStatus());

    assertEquals("TestResource.test", response.readEntity(String.class));
    assertFalse(MDC.getController().isPresent());
  }

  @Path("/test")
  public static class TestResource {
    @GET
    public String test() {
      assertTrue(MDC.getController().isPresent());
      return MDC.getController().get();
    }
  }
}
