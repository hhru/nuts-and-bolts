package ru.hh.nab.example;

import com.google.inject.Singleton;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Singleton
@Path("/")
public class ExampleRs {

  @GET
  @Path("/hello")
  public String hello(@QueryParam("name") @DefaultValue("world") String name){
    return String.format("Hello, %s!", name);
  }
}
