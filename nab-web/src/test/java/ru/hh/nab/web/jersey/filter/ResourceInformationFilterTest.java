package ru.hh.nab.web.jersey.filter;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.OK;
import java.util.HashMap;
import java.util.Map;
import org.glassfish.jersey.server.ResourceConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import ru.hh.nab.common.constants.RequestAttributes;
import ru.hh.nab.common.mdc.MDC;
import ru.hh.nab.web.NabWebTestConfig;

@SpringBootTest(
    classes = ResourceInformationFilterTest.TestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.jersey.application-path=/test"
)
public class ResourceInformationFilterTest {

  @Inject
  private TestRestTemplate testRestTemplate;

  @Test
  public void testResourceName() {
    assertFalse(MDC.getController().isPresent());

    ResponseEntity<String> response = testRestTemplate.getForEntity("/test/test", String.class);

    assertEquals(OK.getStatusCode(), response.getStatusCode().value());

    assertEquals("TestResource#test", response.getBody());
    assertFalse(MDC.getController().isPresent());
  }

  @Test
  public void testContext() {
    String route = "/test/test/context/{name}";
    var response = testRestTemplate.getForEntity(route, Map.class, "test");
    assertEquals(OK.getStatusCode(), response.getStatusCode().value());

    Map<String, String> responseMap = response.getBody();
    assertEquals("testContext", responseMap.get(RequestAttributes.CODE_FUNCTION));
    assertEquals("ru.hh.nab.web.jersey.filter.ResourceInformationFilterTest.TestResource", responseMap.get(RequestAttributes.CODE_NAMESPACE));
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
  @Import(NabWebTestConfig.class)
  public static class TestConfiguration {

    @Bean
    public ResourceConfig resourceConfig() {
      ResourceConfig resourceConfig = new ResourceConfig();
      resourceConfig.register(TestResource.class);
      resourceConfig.register(ResourceInformationFilter.class);
      return resourceConfig;
    }
  }
}
