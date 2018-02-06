package ru.hh.nab.core;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/stats")
public class StatsResource {
  @GET
  @Produces("text/csv")
  public String stats() {
    return "status,ok";
  }
}
