package ru.hh.nab.example;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.starter.servlet.NabJerseyConfig;
import ru.hh.nab.starter.NabServletContextConfig;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {NabTestConfig.class})
public class ExampleResourceTest extends NabTestBase {

  @Test
  public void hello() {
    final String name = "test";
    Response response = target("/hello")
        .queryParam("name", name)
        .request()
        .get();
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals(String.format("Hello, %s!", name), response.readEntity(String.class));
  }

  @Test
  public void helloWithoutParams() {
    Response response = createRequest("/hello").get();
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Hello, world!", response.readEntity(String.class));
  }

  @Override
  protected NabServletContextConfig getServletConfig() {
    return new NabServletContextConfig() {
      @Override
      protected NabJerseyConfig getJerseyConfig() {
        return NabJerseyConfig.forResources(ExampleResource.class);
      }
    };
  }
}
