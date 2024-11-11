package ru.hh.nab.web.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.function.Supplier;

@Path("")
public class StatusResource {
  
  private final String serviceName;
  private final String serviceVersion;
  private final Supplier<Duration> upTimeSupplier;

  public StatusResource(String serviceName, String serviceVersion, Supplier<Duration> upTimeSupplier) {
    this.serviceName = serviceName;
    this.serviceVersion = serviceVersion;
    this.upTimeSupplier = upTimeSupplier;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  public String status() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<project name=\"" + serviceName + "\">\n"
        + " <version>" + serviceVersion + "</version>\n"
        + " <uptime>" + upTimeSupplier.get().toSeconds() + "</uptime>\n"
        + "</project>\n";
  }
}
