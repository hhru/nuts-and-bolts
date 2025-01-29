package ru.hh.nab.web.jersey.resolver;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@Path("/")
@Produces({APPLICATION_XML, APPLICATION_JSON})
public class TestResource {
  @GET
  public DTO simple() {
    return new DTO("test");
  }

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

  @Path("/special")
  @GET
  public DTO special() {
    return new DTO("&<");
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
