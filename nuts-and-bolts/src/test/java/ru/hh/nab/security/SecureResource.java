package ru.hh.nab.security;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class SecureResource {
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
}
