package ru.hh.nab.starter.requestscope;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

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
