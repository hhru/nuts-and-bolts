package ru.hh.nab.starter.resource;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import ru.hh.nab.starter.AppMetadata;

@Path("")
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
