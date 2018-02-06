package ru.hh.nab;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/status")
@Singleton
public class StatusResource {
  
  private final AppMetadata appMetaData;

  @Inject
  public StatusResource(AppMetadata appMetaData) throws IOException {
    this.appMetaData = appMetaData;
  }

  @GET
  @Produces("text/xml")
  public String status() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
    + "<project name=\"" + appMetaData.getName() + "\">\n"
    + " <version>" + appMetaData.getVersion() + "</version>\n"
    + " <uptime>" + ((System.currentTimeMillis() - appMetaData.getStarted()) / 1000) + "</uptime>\n"
    + "</project>\n";
  }
}
