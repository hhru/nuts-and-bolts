package ru.hh.nab.starter.servlet;

import jakarta.servlet.Servlet;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.starter.AppMetadata;
import ru.hh.nab.starter.resource.StatusResource;

// TODO: выпилить этот конфиг
public class StatusServletConfig implements NabServletConfig {
  @Override
  public String[] getMapping() {
    return new String[]{"/status"};
  }

  @Override
  public String getName() {
    return "status";
  }

  @Override
  public Servlet createServlet(WebApplicationContext rootCtx) {
    ResourceConfig statusResourceConfig = new ResourceConfig();
    statusResourceConfig.register(new StatusResource(rootCtx.getBean(AppMetadata.class)));
    return new ServletContainer(statusResourceConfig);
  }
}
