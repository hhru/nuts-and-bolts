package ru.hh.nab.web.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.hh.errors.common.Errors;
import ru.hh.nab.web.NabWebTestConfig;

@SpringBootTest(classes = CustomExceptionMappersTest.CustomExceptionMapperConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CustomExceptionMappersTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Inject
  private TestRestTemplate testRestTemplate;

  @Test
  public void testCustomExceptionMappers() throws IOException {
    ResponseEntity<String> response = testRestTemplate.getForEntity("/iae", String.class);

    assertEquals(SERVICE_UNAVAILABLE.getStatusCode(), response.getStatusCode().value());
    assertEquals("Failed: IAE", response.getBody());
    assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());

    response = testRestTemplate.getForEntity("/any", String.class);

    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode().value());
    assertEquals("Any exception", getErrorDescription(response));
    assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());

    response = testRestTemplate.getForEntity("/any?customSerializer=true", String.class);

    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode().value());
    assertEquals("{\"reason\":\"Any exception\"}", response.getBody());
    assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
  }

  private String getErrorDescription(ResponseEntity<String> response) throws IOException {
    return MAPPER.readValue(response.getBody(), Errors.class).getErrors().get(0).description;
  }

  @Configuration
  @Import({
      NabWebTestConfig.class,
      CustomExceptionSerializer.class,
  })
  public static class CustomExceptionMapperConfig {

    @Bean
    public ResourceConfig resourceConfig() {
      ResourceConfig resourceConfig = new ResourceConfig();
      resourceConfig.register(TestResource.class);
      resourceConfig.register(AnyExceptionMapper.class);
      resourceConfig.register(CustomExceptionMapper.class);
      return resourceConfig;
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
