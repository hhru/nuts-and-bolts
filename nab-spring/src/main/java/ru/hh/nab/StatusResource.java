package ru.hh.nab;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/status")
public class StatusResource {
  
  private final AppMetadata appMetaData;

  public StatusResource(AppMetadata appMetaData) {
    this.appMetaData = appMetaData;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  public String status() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<project name=\"" + appMetaData.getName() + "\">\n"
        + " <version>" + appMetaData.getVersion() + "</version>\n"
        + " <uptime>" + appMetaData.getUpTimeSeconds() + "</uptime>\n"
        + "</project>\n";
  }
}
