package ru.hh.nab;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class JerseyResource {
  @GET
  @Path("/test")
  public String test() {
    return "ok";
  }
}
