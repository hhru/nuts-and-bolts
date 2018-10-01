package ru.hh.nab.starter.filters;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.starter.servlet.DefaultServletConfig;
import ru.hh.nab.starter.servlet.ServletConfig;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {NabTestConfig.class})
public class NabExceptionMappersTest extends NabTestBase {
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
  public void testNabExceptionMappers() {
    Response response = executeGet("/iae");

    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals("IAE", response.readEntity(String.class));
    assertEquals(TEXT_PLAIN_TYPE, response.getMediaType());

    response = executeGet("/ise");

    assertEquals(CONFLICT.getStatusCode(), response.getStatus());
    assertEquals("ISE", response.readEntity(String.class));
    assertEquals(TEXT_PLAIN_TYPE, response.getMediaType());

    response = executeGet("/se");

    assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());
    assertEquals("SE", response.readEntity(String.class));
    assertEquals(TEXT_PLAIN_TYPE, response.getMediaType());

    response = executeGet("/wae");

    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());
    assertEquals(TEXT_HTML_TYPE, new MediaType(response.getMediaType().getType(), response.getMediaType().getSubtype()));

    response = executeGet("/any");

    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    assertEquals("Any exception", response.readEntity(String.class));
    assertEquals(TEXT_PLAIN_TYPE, response.getMediaType());

    response = executeGet("/notfound");

    assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    assertEquals(TEXT_HTML_TYPE, new MediaType(response.getMediaType().getType(), response.getMediaType().getSubtype()));
  }

  @Path("/")
  public static class TestResource {
    @Path("/iae")
    public Response iae() {
      throw new IllegalArgumentException("IAE");
    }

    @Path("/ise")
    public Response ise() {
      throw new IllegalStateException("ISE");
    }

    @Path("/se")
    public Response se() {
      throw new SecurityException("SE");
    }

    @Path("/wae")
    public Response wae() {
      throw new WebApplicationException("WAE", 401);
    }

    @Path("/any")
    public Response any() {
      throw new RuntimeException("Any exception");
    }
  }
}
