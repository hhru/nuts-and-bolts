package ru.hh.nab;

import ru.hh.nab.security.Secure;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class JerseyResource {
  @GET
  @Secure("authenticate")
  @Path("/authenticate")
  public String authenticate() {
    return "ok";
  }

  @GET
  @Secure("anonymous")
  @Path("/anonymous")
  public String anonymous() {
    return "ok";
  }

  @GET
  @Path("/test")
  public String test() {
    return "ok";
  }
}
