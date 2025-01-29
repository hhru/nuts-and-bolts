package ru.hh.nab.web.exceptions;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.exception.JDBCConnectionException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import ru.hh.errors.common.Errors;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.executor.MonitoredThreadPoolExecutor;
import ru.hh.nab.web.NabWebTestConfig;
import static ru.hh.nab.web.http.HttpStatus.SERVICE_PARTIALLY_UNAVAILABLE;

@SpringBootTest(classes = NabExceptionMappersTest.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NabExceptionMappersTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Inject
  private TestRestTemplate testRestTemplate;

  @Test
  public void testNabExceptionMappers() throws IOException {
    ResponseEntity<String> response = testRestTemplate.getForEntity("/iae", String.class);

    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatusCode().value());
    assertEquals("IAE", getErrorDescription(response));
    assertEquals(APPLICATION_JSON, response.getHeaders().getContentType());

    response = testRestTemplate.getForEntity("/ise", String.class);

    assertEquals(CONFLICT.getStatusCode(), response.getStatusCode().value());
    assertEquals("ISE", getErrorDescription(response));
    assertEquals(APPLICATION_JSON, response.getHeaders().getContentType());

    response = testRestTemplate.getForEntity("/se", String.class);

    assertEquals(FORBIDDEN.getStatusCode(), response.getStatusCode().value());
    assertEquals("SE", getErrorDescription(response));
    assertEquals(APPLICATION_JSON, response.getHeaders().getContentType());

    response = testRestTemplate.exchange(RequestEntity.get("/wae").accept(TEXT_HTML).build(), String.class);

    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatusCode().value());
    assertEquals(TEXT_HTML, new MediaType(response.getHeaders().getContentType().getType(), response.getHeaders().getContentType().getSubtype()));

    response = testRestTemplate.getForEntity("/connectionTimeout", String.class);

    assertEquals(SERVICE_PARTIALLY_UNAVAILABLE.getStatusCode(), response.getStatusCode().value());

    response = testRestTemplate.getForEntity("/connectionTimeoutWrapped", String.class);

    assertEquals(SERVICE_PARTIALLY_UNAVAILABLE.getStatusCode(), response.getStatusCode().value());

    response = testRestTemplate.getForEntity("/connectionTimeoutWrappedWithIllegalState", String.class);

    assertEquals(SERVICE_PARTIALLY_UNAVAILABLE.getStatusCode(), response.getStatusCode().value());

    response = testRestTemplate.getForEntity("/any", String.class);

    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode().value());
    assertEquals("Any exception", getErrorDescription(response));
    assertEquals(APPLICATION_JSON, response.getHeaders().getContentType());

    response = testRestTemplate.exchange(RequestEntity.get("/notFound").accept(TEXT_HTML).build(), String.class);

    assertEquals(NOT_FOUND.getStatusCode(), response.getStatusCode().value());
    assertEquals(TEXT_HTML, new MediaType(response.getHeaders().getContentType().getType(), response.getHeaders().getContentType().getSubtype()));

    response = testRestTemplate.getForEntity("/rejectedExecution", String.class);

    assertEquals(SERVICE_PARTIALLY_UNAVAILABLE.getStatusCode(), response.getStatusCode().value());
  }

  private String getErrorDescription(ResponseEntity<String> response) throws IOException {
    return MAPPER.readValue(response.getBody(), Errors.class).getErrors().get(0).description;
  }

  @Path("/")
  public static class TestResource {
    @GET
    @Path("/iae")
    public Response iae() {
      throw new IllegalArgumentException("IAE");
    }

    @GET
    @Path("/ise")
    public Response ise() {
      throw new IllegalStateException("ISE");
    }

    @GET
    @Path("/se")
    public Response se() {
      throw new SecurityException("SE");
    }

    @GET
    @Path("/wae")
    public Response wae() {
      throw new WebApplicationException("WAE", 401);
    }

    @GET
    @Path("/any")
    public Response any() {
      throw new RuntimeException("Any exception");
    }

    @GET
    @Path("/connectionTimeout")
    public Response connectionTimeout() throws SQLException {
      throw new SQLTransientConnectionException();
    }

    @GET
    @Path("/connectionTimeoutWrapped")
    public Response connectionTimeoutWrapped() {
      throw new JDBCConnectionException("Could not connect", new SQLTransientConnectionException());
    }

    @GET
    @Path("/connectionTimeoutWrappedWithIllegalState")
    public Response connectionTimeoutWrappedWithIllegal() {
      throw new JDBCConnectionException("Could not connect", new SQLTransientConnectionException(new IllegalStateException()));
    }

    @GET
    @Path("/rejectedExecution")
    public Response rejectedExecution() {
      var properties = new Properties();
      properties.setProperty("minSize", "4");
      properties.setProperty("maxSize", "4");

      var tpe = MonitoredThreadPoolExecutor.create(new FileSettings(properties), "test", mock(StatsDSender.class), "test");

      tpe.execute(TASK);
      tpe.execute(TASK);
      tpe.execute(TASK);
      tpe.execute(TASK);

      try {
        IntStream.range(0, 5).forEach(i -> tpe.execute(TASK));
      } finally {
        LATCH.countDown();
      }

      return Response.ok().build();
    }

    private static final CountDownLatch LATCH = new CountDownLatch(1);
    private static final Runnable TASK = () -> {
      try {
        LATCH.await();
      } catch (InterruptedException e) {
        //
      }
    };
  }

  @Configuration
  @Import({NabWebTestConfig.class})
  public static class TestConfiguration {

    @Bean
    public ResourceConfig resourceConfig() {
      ResourceConfig resourceConfig = new ResourceConfig();
      resourceConfig.register(TestResource.class);
      resourceConfig.register(AnyExceptionMapper.class);
      resourceConfig.register(IllegalArgumentExceptionMapper.class);
      resourceConfig.register(IllegalStateExceptionMapper.class);
      resourceConfig.register(SecurityExceptionMapper.class);
      resourceConfig.register(WebApplicationExceptionMapper.class);
      return resourceConfig;
    }
  }
}
