package ru.hh.nab.telemetry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class TestResource {

  @GET
  @Path("/simple")
  public String simple() {
    return "Hello, world!";
  }

  @GET
  @Path("/error")
  public String error() {
    throw new RuntimeException("Error description!");
  }
}
