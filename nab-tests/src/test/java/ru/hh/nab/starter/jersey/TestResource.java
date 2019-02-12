package ru.hh.nab.starter.jersey;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

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
