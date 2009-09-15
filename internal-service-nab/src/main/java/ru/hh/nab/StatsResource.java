package ru.hh.nab;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import ru.hh.util.CsvBuilder;

@Path("/stats")
public class StatsResource {
  @GET
  @Produces("text/csv")
  public String stats() {
    return new CsvBuilder().put("status", "ok").build();
  }
}
