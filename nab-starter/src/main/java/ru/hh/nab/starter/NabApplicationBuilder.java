package ru.hh.nab.starter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.starter.servlet.NabJerseyConfig;
import ru.hh.nab.starter.servlet.NabServletConfig;

public final class NabApplicationBuilder {
  private static final String[] ROOT_MAPPING = {"/*"};

  private final Class<?>[] configs;
  private final List<ServletBuilder> servletBuilders;
  private final List<Function<WebApplicationContext, ServletContextListener>> listenerProviders;
  private final List<BiConsumer<ServletContext, WebApplicationContext>> servletContextConfigurers;
  private BiConsumer<ServletContextHandler, WebApplicationContext> servletContextHandlerConfigurer;
  private JerseyBuilder jerseyBuilder;
  private String contextPath;
  private ClassLoader classLoader;

  NabApplicationBuilder(Class<?>[] configs) {
    this.configs = configs;
    servletBuilders = new ArrayList<>();
    servletContextConfigurers = new ArrayList<>();
    listenerProviders = new ArrayList<>();
  }

  public NabApplication build() {
    return new NabApplication(new NabServletContextConfig() {

      @Override
      protected ClassLoader getClassLoader() {
        if (classLoader == null) {
          return super.getClassLoader();
        }
        return classLoader;
      }

      @Override
      protected String getContextPath() {
        return StringUtils.isEmpty(contextPath) ? super.getContextPath() : contextPath;
      }

      @Override
      protected void configureWebapp(ServletContextHandler servletContextHandler, WebApplicationContext rootCtx) {
        Optional.ofNullable(servletContextHandlerConfigurer).ifPresent(cfg -> cfg.accept(servletContextHandler, rootCtx));
      }

      @Override
      protected List<ServletContextListener> getListeners(WebApplicationContext rootCtx) {
        return listenerProviders.stream().map(provider -> provider.apply(rootCtx)).collect(Collectors.toList());
      }

      @Override
      protected List<NabServletConfig> getServletConfigs(WebApplicationContext rootCtx) {
        return ServletBuilder.prepareNabServletConfigs(servletBuilders);
      }

      @Override
      protected NabJerseyConfig getJerseyConfig() {
        if (jerseyBuilder == null) {
          return super.getJerseyConfig();
        }
        return JerseyBuilder.prepareNabJerseyConfig(jerseyBuilder);
      }
    }, configs);
  }

  public NabApplicationBuilder setContextPath(String contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  public NabApplicationBuilder setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  public NabApplicationBuilder addListener(ServletContextListener listener) {
    listenerProviders.add(ctx -> listener);
    return this;
  }

  public NabApplicationBuilder addListenerBean(Function<WebApplicationContext, ServletContextListener> listenerProvider) {
    listenerProviders.add(listenerProvider);
    return this;
  }

  public <F extends Filter> FilterBuilder addFilter(Class<F> filterClass) {
    return new FilterBuilder(this, filterClass);
  }

  public <F extends Filter> FilterBuilder addFilter(F filter, Class<? super F> filterClass) {
    return new FilterBuilder(ctx -> filter, this);
  }

  public <F extends Filter> FilterBuilder addFilterBean(Function<WebApplicationContext, F> filterProvider) {
    return new FilterBuilder(filterProvider, this);
  }

  public FilterBuilder addFilter(FilterHolder filterHolder) {
    return new FilterBuilder(this, ctx -> filterHolder);
  }

  public FilterBuilder addFilterHolderBean(Function<WebApplicationContext, FilterHolder> filterProvider) {
    return new FilterBuilder(this, filterProvider);
  }

  public NabApplicationBuilder configureWebapp(BiConsumer<ServletContextHandler, WebApplicationContext> servletContextHandlerConfigurer) {
    this.servletContextHandlerConfigurer = servletContextHandlerConfigurer;
    return this;
  }

  public ServletBuilder addServlet(Function<WebApplicationContext, Servlet> servletInitializer, Class<?>... childConfigs) {
    return new ServletBuilder(this, servletInitializer, childConfigs);
  }

  public JerseyBuilder configureJersey() {
    return new JerseyBuilder(this);
  }

  public NabApplicationBuilder addServlet(ServletBuilder servletBuilder) {
    servletBuilders.add(servletBuilder);
    return this;
  }


  public NabApplicationBuilder configureJersey(JerseyBuilder jerseyBuilder) {
    this.jerseyBuilder = jerseyBuilder;
    return this;
  }

  private NabApplicationBuilder acceptFilter(FilterBuilder filterBuilder) {
    servletContextConfigurers.add(filterBuilder.registrationAction);
    return this;
  }

  public static final class FilterBuilder {
    private final NabApplicationBuilder nabApplicationBuilder;
    private final BiConsumer<ServletContext, WebApplicationContext> registrationAction;
    private final Map<String, String> initParameters;
    private String[] mappings;
    private String filterName;
    private EnumSet<DispatcherType> dispatcherTypes;

    private <F extends Filter> FilterBuilder(NabApplicationBuilder nabApplicationBuilder, Class<F> filterClass) {
      this.nabApplicationBuilder = nabApplicationBuilder;
      initParameters = new HashMap<>();
      dispatcherTypes = EnumSet.allOf(DispatcherType.class);
      registrationAction = (servletContext, ctx) -> {
        NabServletContextConfig.registerFilter(
          servletContext,
          !StringUtils.isEmpty(filterName) ? filterName : filterClass.getName(),
          filterClass,
          initParameters,
          dispatcherTypes,
          mappings
        );
      };
    }

    private <F extends Filter> FilterBuilder(Function<WebApplicationContext, F> filterProvider, NabApplicationBuilder nabApplicationBuilder) {
      this.nabApplicationBuilder = nabApplicationBuilder;
      initParameters = new HashMap<>();
      dispatcherTypes = EnumSet.allOf(DispatcherType.class);
      registrationAction = (servletContext, ctx) -> {
        F filter = filterProvider.apply(ctx);
        NabServletContextConfig.registerFilter(
          servletContext,
          !StringUtils.isEmpty(filterName) ? filterName : filter.getClass().getName(),
          filter,
          dispatcherTypes,
          mappings
        );
      };
    }

    private FilterBuilder(NabApplicationBuilder nabApplicationBuilder,
      Function<WebApplicationContext, FilterHolder> filterProvider) {
      this.nabApplicationBuilder = nabApplicationBuilder;
      initParameters = new HashMap<>();
      dispatcherTypes = EnumSet.allOf(DispatcherType.class);
      registrationAction = (servletContext, ctx) -> {
        FilterHolder filterHolder = filterProvider.apply(ctx);
        initParameters.forEach(filterHolder::setInitParameter);
        NabServletContextConfig.registerFilter(
          servletContext,
          !StringUtils.isEmpty(filterName) ? filterName : filterHolder.getName(),
          filterHolder,
          dispatcherTypes,
          mappings
        );
      };
    }

    public FilterBuilder setFilterName(String filterName) {
      this.filterName = filterName;
      return this;
    }

    public FilterBuilder addInitParameter(String key, String value) {
      this.initParameters.put(key, value);
      return this;
    }

    public FilterBuilder setDispatchTypes(EnumSet<DispatcherType> dispatcherTypes) {
      this.dispatcherTypes = EnumSet.copyOf(dispatcherTypes);
      return this;
    }

    public NabApplicationBuilder applyTo(String... mappings) {
      this.mappings = mappings;
      return nabApplicationBuilder.acceptFilter(this);
    }

    public NabApplicationBuilder applyToRoot() {
      this.mappings = ROOT_MAPPING;
      return nabApplicationBuilder.acceptFilter(this);
    }
  }

  public static final class ServletBuilder {

    private final NabApplicationBuilder nabApplicationBuilder;
    private final Class<?>[] childConfigurations;
    private final Function<WebApplicationContext, Servlet> servletInitializer;
    private String[] mappings;
    private String servletName;

    public ServletBuilder(NabApplicationBuilder nabApplicationBuilder, Function<WebApplicationContext, Servlet> servletInitializer,
      Class<?>... childConfigurations) {
      this.nabApplicationBuilder = nabApplicationBuilder;
      this.servletInitializer = servletInitializer;
      this.childConfigurations = childConfigurations;
    }

    public ServletBuilder setServletName(String servletName) {
      this.servletName = servletName;
      return this;
    }

    public NabApplicationBuilder applyTo(String... mappings) {
      this.mappings = mappings;
      return nabApplicationBuilder.addServlet(this);
    }

    public NabApplicationBuilder applyToRoot() {
      this.mappings = ROOT_MAPPING;
      return nabApplicationBuilder.addServlet(this);
    }

    private static List<NabServletConfig> prepareNabServletConfigs(List<ServletBuilder> servletBuilders) {
      return servletBuilders.stream().map(servletBuilder -> new NabServletConfig() {
        @Override
        public String[] getMapping() {
          return servletBuilder.mappings;
        }

        @Override
        public String getName() {
          if (StringUtils.isEmpty(servletBuilder.servletName)) {
            return String.join("", getMapping());
          }
          return servletBuilder.servletName;
        }

        @Override
        public Servlet createServlet(WebApplicationContext rootCtx) {
          WebApplicationContext context = createActiveChildCtx(rootCtx, servletBuilder.childConfigurations);
          return servletBuilder.servletInitializer.apply(context);
        }
      }).collect(Collectors.toList());
    }
  }

  public static final class JerseyBuilder {
    private final NabApplicationBuilder nabApplicationBuilder;
    private String[] mappings;
    private final Set<String> allowedPackages;
    private String servletName;
    private final List<Consumer<ResourceConfig>> configurationActions;

    public JerseyBuilder(NabApplicationBuilder nabApplicationBuilder) {
      this.nabApplicationBuilder = nabApplicationBuilder;
      allowedPackages = new HashSet<>();
      configurationActions = new ArrayList<>();
    }

    public JerseyBuilder registerResources(Class<?>... resources) {
      configurationActions.add(resourceConfig -> Arrays.stream(resources).forEach(resourceConfig::register));
      return this;
    }

    public JerseyBuilder registerResourceWithContracts(Class<?> resource, Class<?>... contracts) {
      configurationActions.add(resourceConfig -> resourceConfig.register(resource, contracts));
      return this;
    }

    public JerseyBuilder registerProperty(String key, Object value) {
      configurationActions.add(resourceConfig -> resourceConfig.property(key, value));
      return this;
    }

    public JerseyBuilder executeOnConfig(Consumer<ResourceConfig> configurationAction) {
      configurationActions.add(configurationAction);
      return this;
    }

    public JerseyBuilder setServletName(String servletName) {
      this.servletName = servletName;
      return this;
    }

    public JerseyBuilder addAllowedPackages(String... allowedPackages) {
      this.allowedPackages.addAll(Arrays.asList(allowedPackages));
      return this;
    }

    public NabApplicationBuilder applyTo(String... mappings) {
      this.mappings = mappings;
      return nabApplicationBuilder.configureJersey(this);
    }

    public NabApplicationBuilder applyToRoot() {
      this.mappings = ROOT_MAPPING;
      return nabApplicationBuilder.configureJersey(this);
    }

    private static NabJerseyConfig prepareNabJerseyConfig(JerseyBuilder jerseyBuilder) {
      return new NabJerseyConfig() {
        @Override
        public String[] getMapping() {
          if (jerseyBuilder.mappings.length == 0) {
            return super.getMapping();
          }
          return jerseyBuilder.mappings;
        }

        @Override
        public String getName() {
          if (StringUtils.isEmpty(jerseyBuilder.servletName)) {
            return super.getName();
          }
          return jerseyBuilder.servletName;
        }

        @Override
        public Set<String> getAllowedPackages() {
          if (jerseyBuilder.allowedPackages.isEmpty()) {
            return super.getAllowedPackages();
          }
          return jerseyBuilder.allowedPackages;
        }

        @Override
        public void configure(ResourceConfig resourceConfig) {
          jerseyBuilder.configurationActions.forEach(action -> action.accept(resourceConfig));
        }
      };
    }
  }
}


