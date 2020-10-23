package ru.hh.nab.starter.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.servlet.Servlet;
import javax.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.starter.NabApplicationBuilder;
import ru.hh.nab.starter.jersey.DefaultResourceConfig;
import ru.hh.nab.starter.spring.HierarchicalWebApplicationContext;
import static ru.hh.nab.starter.NabServletContextConfig.DEFAULT_MAPPING;

public abstract class NabJerseyConfig implements NabServletConfig {

  public static final NabJerseyConfig DISABLED = new NabJerseyConfig(true) {
    @Override
    public void configure(WebApplicationContext ctx, ResourceConfig resourceConfig) { }
  };

  private final Class<?>[] childContexts;
  private final boolean disabled;

  protected NabJerseyConfig(Class<?>... childContexts) {
    disabled = false;
    this.childContexts = childContexts;
  }

  private NabJerseyConfig(boolean disabled) {
    this.disabled = disabled;
    childContexts = new Class<?>[0];
  }

  public static NabJerseyConfig forResources(Class<?>... resources) {
    return new NabJerseyConfig() {
      @Override
      public void configure(WebApplicationContext ctx, ResourceConfig resourceConfig) {
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
    Map.Entry<WebApplicationContext, ResourceConfig> ctxtsEntry = createResourceConfig(rootCtx, childContexts);
    configure(ctxtsEntry.getKey(), ctxtsEntry.getValue());
    return new ServletContainer(ctxtsEntry.getValue());
  }

  public Set<String> getAllowedPackages() {
    return Collections.singleton("ru.hh");
  }

  public abstract void configure(WebApplicationContext ctx, ResourceConfig resourceConfig);

  @Override
  public final boolean isDisabled() {
    return disabled;
  }

  private Map.Entry<WebApplicationContext, ResourceConfig> createResourceConfig(WebApplicationContext rootCtx, Class<?>... childContexts) {
    ResourceConfig resourceConfig = new DefaultResourceConfig();
    HierarchicalWebApplicationContext jerseyContext = new HierarchicalWebApplicationContext(rootCtx);
    if (childContexts.length > 0) {
      jerseyContext.register(childContexts);
    }
    jerseyContext.setParent(rootCtx);
    jerseyContext.refresh();
    jerseyContext.getBeansWithAnnotation(javax.ws.rs.Path.class).values().stream()
      .filter(bean -> getAllowedPackages().stream().anyMatch(allowedPackage -> bean.getClass().getName().startsWith(allowedPackage)))
      .forEach(resourceConfig::register);
    return Map.entry(jerseyContext, resourceConfig);
  }

  public static final class Builder {
    private final NabApplicationBuilder nabApplicationBuilder;
    private final Class<?>[] childConfigs;
    private String[] mappings;
    private final Set<String> allowedPackages;
    private String servletName;
    private final List<BiConsumer<WebApplicationContext, ResourceConfig>> configurationActions;

    public Builder(NabApplicationBuilder nabApplicationBuilder, Class<?>... childConfigs) {
      this.nabApplicationBuilder = nabApplicationBuilder;
      this.childConfigs = childConfigs;
      allowedPackages = new HashSet<>();
      configurationActions = new ArrayList<>();
    }

    public Builder registerResources(Class<?>... resources) {
      if (Stream.of(resources).anyMatch(cls -> cls.isAnnotationPresent(Path.class))) {
        throw new IllegalArgumentException("Endpoints must be registered with Spring context. " +
          "The method should be used to register pure jersey components with minimal dependencies");
      }
      configurationActions.add((ctx, resourceConfig) -> Arrays.stream(resources).forEach(resourceConfig::register));
      return this;
    }

    public Builder registerResourceBean(Function<WebApplicationContext, ?> resourceProvider) {
      configurationActions.add((ctx, resourceConfig) -> resourceConfig.register(resourceProvider.apply(ctx)));
      return this;
    }

    public Builder registerResourceWithContracts(Class<?> resource, Class<?>... contracts) {
      configurationActions.add((ctx, resourceConfig) -> resourceConfig.register(resource, contracts));
      return this;
    }

    public Builder registerProperty(String key, Object value) {
      configurationActions.add((ctx, resourceConfig) -> resourceConfig.property(key, value));
      return this;
    }

    public Builder executeOnConfig(Consumer<ResourceConfig> configurationAction) {
      configurationActions.add((ctx, resourceConfig) -> configurationAction.accept(resourceConfig));
      return this;
    }

    public Builder setServletName(String servletName) {
      this.servletName = servletName;
      return this;
    }

    public Builder addAllowedPackages(String... allowedPackages) {
      this.allowedPackages.addAll(Arrays.asList(allowedPackages));
      return this;
    }

    public NabApplicationBuilder bindTo(String... mappings) {
      this.mappings = mappings;
      return nabApplicationBuilder.configureJersey(this);
    }

    public NabApplicationBuilder bindToRoot() {
      return bindTo(NabApplicationBuilder.ROOT_MAPPING);
    }

    public NabJerseyConfig build() {
      return new NabJerseyConfig(childConfigs) {
        @Override
        public String[] getMapping() {
          if (mappings.length == 0) {
            return super.getMapping();
          }
          return mappings;
        }

        @Override
        public String getName() {
          if (StringUtils.isEmpty(servletName)) {
            return super.getName();
          }
          return servletName;
        }

        @Override
        public Set<String> getAllowedPackages() {
          if (allowedPackages.isEmpty()) {
            return super.getAllowedPackages();
          }
          return allowedPackages;
        }

        @Override
        public void configure(WebApplicationContext ctx, ResourceConfig resourceConfig) {
          configurationActions.forEach(action -> action.accept(ctx, resourceConfig));
        }
      };
    }
  }
}
