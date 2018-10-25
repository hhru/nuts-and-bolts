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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {NabTestConfig.class})
public class FilteredXMLTest extends NabTestBase {
  @Override
  protected NabApplication getApplication() {
    return NabApplication.builder().configureJersey().registerResources(TestResource.class).bindToRoot().build();
  }

  @Test
  public void testFilteredXML() {
    Response response = createRequest("/0C").accept(APPLICATION_XML).get();
    assertEquals("\uFFFD", response.readEntity(DTO.class).string);

    response = createRequest("/FFFE").accept(APPLICATION_XML).get();
    assertEquals("\uFFFD", response.readEntity(DTO.class).string);

    response = createRequest("/0A").accept(APPLICATION_XML).get();
    assertEquals("\n", response.readEntity(DTO.class).string);
  }

  @XmlRootElement(name = "dto")
  public static class DTO {
    @XmlValue
    public String string;

    public DTO() {}

    public DTO(String string) {
      this.string = string;
    }
  }

  @Path("/")
  @Produces(APPLICATION_XML)
  public static class TestResource {
    @Path("/0C")
    @GET
    public DTO c() {
      return new DTO("\u000C");
    }

    @Path("/FFFE")
    @GET
    public DTO fffe() {
      return new DTO("\uFFFE");
    }

    @Path("/0A")
    @GET
    public DTO a() {
      return new DTO("\n");
    }
  }
}
