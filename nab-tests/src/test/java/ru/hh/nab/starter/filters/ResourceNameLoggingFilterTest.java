package ru.hh.nab.starter.filters;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.mdc.MDC;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.testbase.extensions.NabJunitWebConfig;
import ru.hh.nab.testbase.extensions.NabTestServer;
import ru.hh.nab.testbase.extensions.OverrideNabApplication;

@NabJunitWebConfig(NabTestConfig.class)
public class ResourceNameLoggingFilterTest {

  @NabTestServer(overrideApplication = SpringCtxForJersey.class)
  ResourceHelper resourceHelper;

  @Test
  public void testResourceName() {
    assertFalse(MDC.getController().isPresent());

    Response response = resourceHelper.executeGet("/test/test");

    assertEquals(OK.getStatusCode(), response.getStatus());

    assertEquals("TestResource#test", response.readEntity(String.class));
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

  @Configuration
  @Import(TestResource.class)
  public static class SpringCtxForJersey implements OverrideNabApplication {
    @Override
    public NabApplication getNabApplication() {
      return NabApplication.builder().configureJersey(SpringCtxForJersey.class).bindTo("/test/*").build();
    }
  }
}
