package ru.hh.nab.starter.requestscope;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class TestResource {

  @Inject
  private Provider<RequestDetails> requestProvider;

  @GET
  @Path("/hello")
  public String hello() {

    return String.format("Hello, %s!", requestProvider.get().getField());
  }
}
