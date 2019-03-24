package ru.hh.nab.starter.exceptions;

import org.hibernate.exception.JDBCConnectionException;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.common.executor.MonitoredThreadPoolExecutor;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@ContextConfiguration(classes = {NabTestConfig.class})
public class NabExceptionMappersTest extends NabTestBase {
  @Override
  protected NabApplication getApplication() {
    return NabApplication.builder().configureJersey().registerResources(TestResource.class).bindToRoot().build();
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

    response = executeGet("/connectionTimeout");

    assertEquals(SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());

    response = executeGet("/connectionTimeoutWrapped");

    assertEquals(SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());

    response = executeGet("/any");

    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    assertEquals("Any exception", response.readEntity(String.class));
    assertEquals(TEXT_PLAIN_TYPE, response.getMediaType());

    response = executeGet("/notFound");

    assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    assertEquals(TEXT_HTML_TYPE, new MediaType(response.getMediaType().getType(), response.getMediaType().getSubtype()));

    response = executeGet("/rejectedExecution");

    assertEquals(SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
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

    @Path("/connectionTimeout")
    public Response connectionTimeout() throws SQLException {
      throw new SQLTransientConnectionException();
    }

    @Path("/connectionTimeoutWrapped")
    public Response connectionTimeoutWrapped() {
      throw new JDBCConnectionException("Could not connect", new SQLTransientConnectionException());
    }

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
}
