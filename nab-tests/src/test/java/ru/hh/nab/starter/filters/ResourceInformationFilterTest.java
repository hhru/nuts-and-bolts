package ru.hh.nab.starter.filters;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.constants.RequestAttributes;
import ru.hh.nab.common.mdc.MDC;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.testbase.extensions.NabJunitWebConfig;
import ru.hh.nab.testbase.extensions.NabTestServer;
import ru.hh.nab.testbase.extensions.OverrideNabApplication;

@NabJunitWebConfig(NabTestConfig.class)
public class ResourceInformationFilterTest {

  @NabTestServer(overrideApplication = SpringCtxForJersey.class)
  ResourceHelper resourceHelper;
  @Context
  HttpServletRequest request;

  @Test
  public void testResourceName() {
    assertFalse(MDC.getController().isPresent());

    Response response = resourceHelper.executeGet("/test/test");

    assertEquals(OK.getStatusCode(), response.getStatus());

    assertEquals("TestResource#test", response.readEntity(String.class));
    assertFalse(MDC.getController().isPresent());
  }

  @Test
  public void testContext() {
    String route = "/test/test/context/{name}";
    Response response = resourceHelper
        .executeGet(resourceHelper.jerseyUrl(route, "test"));
    assertEquals(OK.getStatusCode(), response.getStatus());

    Map<String, String> responseMap = response.readEntity(Map.class);
    assertEquals("testContext", responseMap.get(RequestAttributes.CODE_FUNCTION));
    assertEquals("ru.hh.nab.starter.filters.ResourceInformationFilterTest.TestResource", responseMap.get(RequestAttributes.CODE_NAMESPACE));
    assertEquals(route, responseMap.get(RequestAttributes.HTTP_ROUTE));
  }

  @Path("/test")
  public static class TestResource {
    @GET
    public String test() {
      assertTrue(MDC.getController().isPresent());
      return MDC.getController().get();
    }
    @GET
    @Path("/context/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response testContext(@Context HttpServletRequest request, @PathParam("name") String name) {
      Map<String, Object> result = new HashMap<>();
      result.put(RequestAttributes.CODE_FUNCTION, request.getAttribute(RequestAttributes.CODE_FUNCTION));
      result.put(RequestAttributes.CODE_NAMESPACE, request.getAttribute(RequestAttributes.CODE_NAMESPACE));
      result.put(RequestAttributes.HTTP_ROUTE, request.getAttribute(RequestAttributes.HTTP_ROUTE));
      return Response.ok().entity(result).build();
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
