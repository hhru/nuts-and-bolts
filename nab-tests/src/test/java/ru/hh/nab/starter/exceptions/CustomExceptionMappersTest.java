package ru.hh.nab.starter.exceptions;

import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {NabTestConfig.class, CustomExceptionMappersTest.CustomExceptionMapperConfig.class})
public class CustomExceptionMappersTest extends NabTestBase {
  @Override
  protected NabApplication getApplication() {
    return NabApplication.builder()
      .configureJersey(SpringCtxForJersey.class).registerResources(CustomExceptionMapper.class).bindToRoot()
      .build();
  }

  @Test
  public void testCustomExceptionMappers() {
    Response response = executeGet("/iae");

    assertEquals(SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
    assertEquals("Failed: IAE", response.readEntity(String.class));
    assertEquals(TEXT_PLAIN_TYPE, response.getMediaType());

    response = executeGet("/any");

    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    assertEquals("Any exception", response.readEntity(String.class));
    assertEquals(TEXT_PLAIN_TYPE, response.getMediaType());

    response = executeGet("/any?customSerializer=true");

    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    assertEquals("{\"reason\":\"Any exception\"}", response.readEntity(String.class));
    assertEquals(APPLICATION_JSON_TYPE, response.getMediaType());
  }

  @Configuration
  @Import(CustomExceptionSerializer.class)
  public static class CustomExceptionMapperConfig { }

  public static class CustomExceptionSerializer implements ExceptionSerializer {
    @Override
    public boolean isCompatible(HttpServletRequest request, HttpServletResponse response) {
      return request.getParameter("customSerializer") != null;
    }

    @Override
    public Response serializeException(Response.StatusType statusCode, Exception exception) {
      return Response.status(statusCode).entity(new Error(exception.getMessage())).type(APPLICATION_JSON_TYPE).build();
    }

    public static class Error {
      public String reason;

      public Error(String reason) {
        this.reason = reason;
      }
    }
  }

  public static class CustomExceptionMapper extends NabExceptionMapper<IllegalArgumentException> {
    public CustomExceptionMapper() {
      super(SERVICE_UNAVAILABLE, LoggingLevel.ERROR_WITH_STACK_TRACE);
    }

    @Override
    protected Response serializeException(Response.StatusType statusCode, IllegalArgumentException exception) {
      return Response.status(statusCode).entity("Failed: IAE").type(TEXT_PLAIN_TYPE).build();
    }
  }

  @Path("/")
  public static class TestResource {
    @Path("/iae")
    public Response iae() {
      throw new IllegalArgumentException("IAE");
    }

    @Path("/any")
    public Response any() {
      throw new RuntimeException("Any exception");
    }
  }

  @Configuration
  @Import(TestResource.class)
  static class SpringCtxForJersey {
  }
}
