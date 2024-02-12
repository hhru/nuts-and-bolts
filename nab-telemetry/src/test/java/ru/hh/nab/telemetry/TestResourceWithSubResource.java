package ru.hh.nab.telemetry;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

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
