package ru.hh.nab.web.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import java.io.IOException;
import org.glassfish.jersey.server.ResourceConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import ru.hh.errors.common.Errors;
import ru.hh.nab.testbase.web.WebTestBase;
import ru.hh.nab.web.NabWebTestConfig;

@SpringBootTest(classes = CustomExceptionMappersTest.CustomExceptionMapperConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CustomExceptionMappersTest extends WebTestBase {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void testCustomExceptionMappers() throws IOException {
    Response response = resourceHelper.executeGet("/iae");

    assertEquals(SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
    assertEquals("Failed: IAE", response.readEntity(String.class));
    assertEquals(TEXT_PLAIN_TYPE, response.getMediaType());

    response = resourceHelper.executeGet("/any");

    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    assertEquals("Any exception", getErrorDescription(response));
    assertEquals(APPLICATION_JSON_TYPE, response.getMediaType());

    response = resourceHelper.executeGet("/any?customSerializer=true");

    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    assertEquals("{\"reason\":\"Any exception\"}", response.readEntity(String.class));
    assertEquals(APPLICATION_JSON_TYPE, response.getMediaType());
  }

  private String getErrorDescription(Response response) throws IOException {
    return MAPPER.readValue(response.readEntity(String.class), Errors.class).getErrors().get(0).description;
  }

  @Configuration
  @Import({
      NabWebTestConfig.class,
      CustomExceptionSerializer.class,
      TestResource.class,
  })
  public static class CustomExceptionMapperConfig {

    @Bean
    public ResourceConfig resourceConfig() {
      return new ResourceConfig().register(CustomExceptionMapper.class);
    }
  }

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
}
