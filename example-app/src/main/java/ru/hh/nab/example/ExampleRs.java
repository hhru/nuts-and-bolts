package ru.hh.nab.example;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import ru.hh.nab.health.monitoring.TimingsLogger;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Singleton
@Path("/")
public class ExampleRs {

  @Inject
  private Provider<TimingsLogger> loggerProvider;

  @GET
  @Path("/hello")
  public String hello(@QueryParam("name") @DefaultValue("world") String name){
    loggerProvider.get().probe("hello.entry-point");
    try {
      return String.format("Hello, %s!", name);
    } finally {
      loggerProvider.get().probe("hello.exit-point");
    }
  }
}
