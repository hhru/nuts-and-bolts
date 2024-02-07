package ru.hh.nab.telemetry;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/")
public class TestResource {
  @GET
  public String root() {
    return "root";
  }
  @GET
  @Path("/simple")
  public String simple() {
    return "Hello, world!";
  }

  @GET
  @Path("/simple/{name}/greeting")
  public String simpleWithParam(@PathParam("name") @DefaultValue("world") String name) {
    return "Hello, %s!".formatted(name);
  }

  @GET
  @Path("/error")
  public String error() {
    throw new RuntimeException("Error description!");
  }
}
