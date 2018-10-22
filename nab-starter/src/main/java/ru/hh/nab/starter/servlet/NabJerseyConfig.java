package ru.hh.nab.starter.servlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import javax.servlet.Servlet;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.starter.jersey.DefaultResourceConfig;
import static ru.hh.nab.starter.NabServletContextConfig.DEFAULT_MAPPING;

public abstract class NabJerseyConfig implements NabServletConfig {

  public static final NabJerseyConfig DISABLED = new NabJerseyConfig(true) {
    @Override
    public void configure(ResourceConfig resourceConfig) { }
  };

  private final boolean disabled;

  protected NabJerseyConfig() {
    this.disabled = false;
  }

  private NabJerseyConfig(boolean disabled) {
    this.disabled = disabled;
  }

  public static NabJerseyConfig forResources(Class<?>... resources) {
    return new NabJerseyConfig() {
      @Override
      public void configure(ResourceConfig resourceConfig) {
        Arrays.stream(resources).forEach(resourceConfig::register);
      }
    };
  }

  @Override
  public String[] getMapping() {
    return DEFAULT_MAPPING;
  }

  @Override
  public String getName() {
    return "jersey";
  }

  @Override
  public Servlet createServlet(WebApplicationContext rootCtx) {
    ResourceConfig resourceConfig = createResourceConfig(rootCtx);
    configure(resourceConfig);
    return new ServletContainer(resourceConfig);
  }

  public Set<String> getAllowedPackages() {
    return Collections.singleton("ru.hh");
  }

  public abstract void configure(ResourceConfig resourceConfig);

  @Override
  public final boolean isDisabled() {
    return disabled;
  }

  private ResourceConfig createResourceConfig(WebApplicationContext ctx) {
    ResourceConfig resourceConfig = new DefaultResourceConfig();
    ctx.getBeansWithAnnotation(javax.ws.rs.Path.class).values().stream()
      .filter(bean -> getAllowedPackages().stream().anyMatch(allowedPackage -> bean.getClass().getName().startsWith(allowedPackage)))
      .forEach(resourceConfig::register);
    return resourceConfig;
  }
}
