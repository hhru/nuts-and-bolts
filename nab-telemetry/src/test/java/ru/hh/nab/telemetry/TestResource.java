package ru.hh.nab.telemetry;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

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
