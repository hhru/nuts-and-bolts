package ru.hh.nab.starter.resource;

import ru.hh.nab.starter.AppMetadata;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/status")
@Singleton
public class StatusResource {
  
  private final AppMetadata appMetaData;

  @Inject
  public StatusResource(AppMetadata appMetaData) {
    this.appMetaData = appMetaData;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  public String status() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<project name=\"" + appMetaData.getServiceName() + "\">\n"
        + " <version>" + appMetaData.getVersion() + "</version>\n"
        + " <uptime>" + appMetaData.getUpTimeSeconds() + "</uptime>\n"
        + "</project>\n";
  }
}
