package ru.hh.nab;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/stats")
@Singleton
public class StatsResource {
  @GET
  @Produces("text/csv")
  public String stats() {
    return "status,ok";
  }
}
