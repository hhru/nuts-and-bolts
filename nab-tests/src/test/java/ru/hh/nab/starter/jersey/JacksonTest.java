package ru.hh.nab.starter.jersey;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {NabTestConfig.class})
public class JacksonTest extends NabTestBase {
  @Override
  protected NabApplication getApplication() {
    return NabApplication.builder().configureJersey().registerResources(TestResource.class).bindToRoot().build();
  }

  @Test
  public void testJaxsonJaxb() {
    Response response = createRequest("/").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"test\"}", response.readEntity(String.class));
  }

  @Path("/")
  @Produces(APPLICATION_JSON)
  public static class TestResource {
    @GET
    public DTO c() {
      return new DTO("test");
    }
  }

  @XmlRootElement(name = "dto")
  public static class DTO {
    @XmlElement
    public String string;

    public DTO() {}

    public DTO(String string) {
      this.string = string;
    }
  }
}
