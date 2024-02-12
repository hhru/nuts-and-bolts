package ru.hh.nab.telemetry;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/resource")
public class TestResourceWithSubResource {
  @GET
  @Path("/simple/{name}/greeting")
  public String simpleWithParam(@PathParam("name") @DefaultValue("world") String name) {
    return "Hello, %s!".formatted(name);
  }
  @Path("/sub")
  public SubResource simple() {
    return new SubResource();
  }

  public static class SubResource {
    @GET
    @Path("/simple")
    public String simple() {
      return "Hello, world!";
    }
    @HEAD
    @Path("/simple")
    public void simpleHead() {
      //do nothing, just for check duplicated path
    }
    @GET
    @Path("/simple/{name}/greeting")
    public String simpleWithParam(@PathParam("name") @DefaultValue("world") String name) {
      return "Hello, %s!".formatted(name);
    }
  }
}
