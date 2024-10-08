package ru.hh.nab.starter.exceptions;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import org.hibernate.exception.JDBCConnectionException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import ru.hh.errors.common.Errors;
import ru.hh.nab.common.executor.MonitoredThreadPoolExecutor;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.starter.NabApplication;
import static ru.hh.nab.starter.http.HttpStatus.SERVICE_PARTIALLY_UNAVAILABLE;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.testbase.extensions.NabJunitWebConfig;
import ru.hh.nab.testbase.extensions.NabTestServer;
import ru.hh.nab.testbase.extensions.OverrideNabApplication;

@NabJunitWebConfig(NabTestConfig.class)
public class NabExceptionMappersTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @NabTestServer(overrideApplication = SpringCtxForJersey.class)
  ResourceHelper resourceHelper;

  @Test
  public void testNabExceptionMappers() throws IOException {
    Response response = resourceHelper.executeGet("/iae");

    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals("IAE", getErrorDescription(response));
    assertEquals(APPLICATION_JSON_TYPE, response.getMediaType());

    response = resourceHelper.executeGet("/ise");

    assertEquals(CONFLICT.getStatusCode(), response.getStatus());
    assertEquals("ISE", getErrorDescription(response));
    assertEquals(APPLICATION_JSON_TYPE, response.getMediaType());

    response = resourceHelper.executeGet("/se");

    assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());
    assertEquals("SE", getErrorDescription(response));
    assertEquals(APPLICATION_JSON_TYPE, response.getMediaType());

    response = resourceHelper.executeGet("/wae");

    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());
    assertEquals(TEXT_HTML_TYPE, new MediaType(response.getMediaType().getType(), response.getMediaType().getSubtype()));

    response = resourceHelper.executeGet("/connectionTimeout");

    assertEquals(SERVICE_PARTIALLY_UNAVAILABLE.getStatusCode(), response.getStatus());

    response = resourceHelper.executeGet("/connectionTimeoutWrapped");

    assertEquals(SERVICE_PARTIALLY_UNAVAILABLE.getStatusCode(), response.getStatus());

    response = resourceHelper.executeGet("/connectionTimeoutWrappedWithIllegalState");

    assertEquals(SERVICE_PARTIALLY_UNAVAILABLE.getStatusCode(), response.getStatus());

    response = resourceHelper.executeGet("/any");

    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    assertEquals("Any exception", getErrorDescription(response));
    assertEquals(APPLICATION_JSON_TYPE, response.getMediaType());

    response = resourceHelper.executeGet("/notFound");

    assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    assertEquals(TEXT_HTML_TYPE, new MediaType(response.getMediaType().getType(), response.getMediaType().getSubtype()));

    response = resourceHelper.executeGet("/rejectedExecution");

    assertEquals(SERVICE_PARTIALLY_UNAVAILABLE.getStatusCode(), response.getStatus());
  }

  private String getErrorDescription(Response response) throws IOException {
    return MAPPER.readValue(response.readEntity(String.class), Errors.class).getErrors().get(0).description;
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
  @Import(TestResource.class)
  public static class SpringCtxForJersey implements OverrideNabApplication {
    @Override
    public NabApplication getNabApplication() {
      return NabApplication.builder().configureJersey(SpringCtxForJersey.class).bindToRoot().build();
    }
  }
}
