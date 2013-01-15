package ru.hh.nab;

import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/status")
@Singleton
public class StatusResource {
  private final String name;
  private final String version;
  private final long started;

  public StatusResource() throws IOException {
    Properties p = new Properties();
    InputStream s = Launcher.module.getClass().getResourceAsStream("/project.properties");
    if (s != null) {
      p.load(s);
    }

    version = p.getProperty("project.version", "unknown");
    name = p.getProperty("project.name", "unknown");
    started = System.currentTimeMillis();
  }

  @GET
  @Produces("text/xml")
  public String status() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
    + "<project name=\"" + name + "\">\n"
    + " <version>" + version + "</version>\n"
    + " <uptime>" + ((System.currentTimeMillis() - started) / 1000) + "</uptime>\n"
    + "</project>\n";
  }
}
