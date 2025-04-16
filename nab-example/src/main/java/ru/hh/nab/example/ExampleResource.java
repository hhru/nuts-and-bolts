package ru.hh.nab.example;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/")
public class ExampleResource {

  @GET
  @Path("/hello")
  public String hello(@DefaultValue("world") @QueryParam("name") String name) {
    return String.format("Hello, %s!", name);
  }
}
