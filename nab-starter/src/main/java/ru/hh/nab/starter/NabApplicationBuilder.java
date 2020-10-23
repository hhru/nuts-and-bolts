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
import java.util.stream.Stream;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.ws.rs.Path;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.starter.servlet.NabJerseyConfig;
import ru.hh.nab.starter.servlet.NabServletConfig;

public final class NabApplicationBuilder {
  private static final String[] ROOT_MAPPING = {"/*"};

  private final List<ServletBuilder> servletBuilders;
  private final List<Function<WebApplicationContext, ServletContextListener>> listenerProviders;
  private final List<BiConsumer<ServletContext, WebApplicationContext>> servletContextConfigurers;
  private BiConsumer<WebAppContext, WebApplicationContext> servletContextHandlerConfigurer;
  private JerseyBuilder jerseyBuilder;
  private String contextPath;
  private ClassLoader classLoader;

  NabApplicationBuilder() {
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
      protected void configureWebapp(WebAppContext webAppContext, WebApplicationContext rootCtx) {
        Optional.ofNullable(servletContextHandlerConfigurer).ifPresent(cfg -> cfg.accept(webAppContext, rootCtx));
      }

      @Override
      protected void configureServletContext(ServletContext servletContext, WebApplicationContext rootCtx) {
        servletContextConfigurers.forEach(cfg -> cfg.accept(servletContext, rootCtx));
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
        return jerseyBuilder.prepareNabJerseyConfig();
      }
    });
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

  public <F extends Filter> FilterBuilder<F> addFilter(Class<F> filterClass) {
    return new FilterBuilder<>(filterClass);
  }

  public <F extends Filter> FilterProviderBuilder<F> addFilter(F filter) {
    return new FilterProviderBuilder<>(ctx -> filter);
  }

  public <F extends Filter> FilterProviderBuilder<F> addFilterBean(Function<WebApplicationContext, F> filterProvider) {
    return new FilterProviderBuilder<>(filterProvider);
  }

  public FilterHolderBuilder addFilter(FilterHolder filterHolder) {
    return new FilterHolderBuilder(ctx -> filterHolder);
  }

  public FilterHolderBuilder addFilterHolderBean(Function<WebApplicationContext, FilterHolder> filterProvider) {
    return new FilterHolderBuilder(filterProvider);
  }

  public NabApplicationBuilder configureWebapp(BiConsumer<WebAppContext, WebApplicationContext> servletContextHandlerConfigurer) {
    this.servletContextHandlerConfigurer = servletContextHandlerConfigurer;
    return this;
  }

  public ServletBuilder addServlet(Function<WebApplicationContext, Servlet> servletInitializer, Class<?>... childConfigs) {
    return new ServletBuilder(this, servletInitializer, childConfigs);
  }

  public JerseyBuilder configureJersey(Class<?>... childConfigs) {
    return new JerseyBuilder(this, childConfigs);
  }

  public NabApplicationBuilder addServlet(ServletBuilder servletBuilder) {
    servletBuilders.add(servletBuilder);
    return this;
  }


  public NabApplicationBuilder configureJersey(JerseyBuilder jerseyBuilder) {
    this.jerseyBuilder = jerseyBuilder;
    return this;
  }

  private NabApplicationBuilder acceptFilter(AbstractFilterBuilder<?> filterBuilder) {
    servletContextConfigurers.add(filterBuilder::registrationAction);
    return this;
  }

  private abstract class AbstractFilterBuilder<IMPL extends AbstractFilterBuilder<IMPL>> {

    private String[] mappings;
    private String filterName;
    private EnumSet<DispatcherType> dispatcherTypes = EnumSet.allOf(DispatcherType.class);

    abstract IMPL self();

    abstract void registrationAction(ServletContext servletContext, WebApplicationContext webApplicationContext);

    String[] getMappings() {
      return mappings;
    }

    String getFilterName() {
      return filterName;
    }

    EnumSet<DispatcherType> getDispatcherTypes() {
      return dispatcherTypes;
    }

    public IMPL setFilterName(String filterName) {
      this.filterName = filterName;
      return self();
    }

    public IMPL setDispatchTypes(EnumSet<DispatcherType> dispatcherTypes) {
      this.dispatcherTypes = EnumSet.copyOf(dispatcherTypes);
      return self();
    }

    public NabApplicationBuilder bindTo(String... mappings) {
      this.mappings = mappings;
      return acceptFilter(this);
    }

    public NabApplicationBuilder bindToRoot() {
      this.mappings = ROOT_MAPPING;
      return acceptFilter(this);
    }
  }

  private abstract class ParameterizableFilterBuilder<IMPL extends AbstractFilterBuilder<IMPL>> extends AbstractFilterBuilder<IMPL> {
    private final Map<String, String> initParameters = new HashMap<>();

    Map<String, String> getInitParameters() {
      return initParameters;
    }

    public IMPL addInitParameter(String key, String value) {
      this.initParameters.put(key, value);
      return self();
    }
  }

  public final class FilterBuilder<F extends Filter> extends ParameterizableFilterBuilder<FilterBuilder<F>> {

    private final Class<F> filterClass;

    private FilterBuilder(Class<F> filterClass) {
      this.filterClass = filterClass;
    }

    @Override
    FilterBuilder<F> self() {
      return this;
    }

    @Override
    void registrationAction(ServletContext servletContext, WebApplicationContext webApplicationContext) {
      final String filterName = getFilterName();
      NabServletContextConfig.registerFilter(
              servletContext,
              !StringUtils.isEmpty(filterName) ? filterName : filterClass.getName(),
              filterClass,
              getInitParameters(),
              getDispatcherTypes(),
              getMappings()
      );
    }
  }

  public final class FilterProviderBuilder<F extends Filter> extends AbstractFilterBuilder<FilterProviderBuilder<F>> {

    private final Function<WebApplicationContext, F> filterProvider;

    private FilterProviderBuilder(Function<WebApplicationContext, F> filterProvider) {
      this.filterProvider = filterProvider;
    }

    @Override
    FilterProviderBuilder<F> self() {
      return this;
    }

    @Override
    void registrationAction(ServletContext servletContext, WebApplicationContext webApplicationContext) {
      final String filterName = getFilterName();
      F filter = filterProvider.apply(webApplicationContext);
      NabServletContextConfig.registerFilter(
              servletContext,
              !StringUtils.isEmpty(filterName) ? filterName : filter.getClass().getName(),
              filter,
              getDispatcherTypes(),
              getMappings()
      );
    }
  }

  public final class FilterHolderBuilder extends ParameterizableFilterBuilder<FilterHolderBuilder> {

    private final Function<WebApplicationContext, FilterHolder> filterHolderProvider;

    private FilterHolderBuilder(Function<WebApplicationContext, FilterHolder> filterHolderProvider) {
      this.filterHolderProvider = filterHolderProvider;
    }

    @Override
    FilterHolderBuilder self() {
      return this;
    }

    @Override
    void registrationAction(ServletContext servletContext, WebApplicationContext webApplicationContext) {
      final String filterName = getFilterName();
      FilterHolder filterHolder = filterHolderProvider.apply(webApplicationContext);
      getInitParameters().forEach(filterHolder::setInitParameter);
      NabServletContextConfig.registerFilter(
              servletContext,
              !StringUtils.isEmpty(filterName) ? filterName : filterHolder.getName(),
              filterHolder,
              getDispatcherTypes(),
              getMappings()
      );
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

    public NabApplicationBuilder bindTo(String... mappings) {
      this.mappings = mappings;
      return nabApplicationBuilder.addServlet(this);
    }

    public NabApplicationBuilder bindToRoot() {
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
    private final Class<?>[] childConfigs;
    private String[] mappings;
    private final Set<String> allowedPackages;
    private String servletName;
    private final List<BiConsumer<WebApplicationContext, ResourceConfig>> configurationActions;

    public JerseyBuilder(NabApplicationBuilder nabApplicationBuilder, Class<?>... childConfigs) {
      this.nabApplicationBuilder = nabApplicationBuilder;
      this.childConfigs = childConfigs;
      allowedPackages = new HashSet<>();
      configurationActions = new ArrayList<>();
    }

    public JerseyBuilder registerResources(Class<?>... resources) {
      if (Stream.of(resources).anyMatch(cls -> cls.isAnnotationPresent(Path.class))) {
        throw new IllegalArgumentException("Endpoints must be registered with Spring context. " +
          "The method should be used to register pure jersey components with minimal dependencies");
      }
      configurationActions.add((ctx, resourceConfig) -> Arrays.stream(resources).forEach(resourceConfig::register));
      return this;
    }

    public JerseyBuilder registerResourceBean(Function<WebApplicationContext, ?> resourceProvider) {
      configurationActions.add((ctx, resourceConfig) -> resourceConfig.register(resourceProvider.apply(ctx)));
      return this;
    }

    public JerseyBuilder registerResourceWithContracts(Class<?> resource, Class<?>... contracts) {
      configurationActions.add((ctx, resourceConfig) -> resourceConfig.register(resource, contracts));
      return this;
    }

    public JerseyBuilder registerProperty(String key, Object value) {
      configurationActions.add((ctx, resourceConfig) -> resourceConfig.property(key, value));
      return this;
    }

    public JerseyBuilder executeOnConfig(Consumer<ResourceConfig> configurationAction) {
      configurationActions.add((ctx, resourceConfig) -> configurationAction.accept(resourceConfig));
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

    public NabApplicationBuilder bindTo(String... mappings) {
      this.mappings = mappings;
      return nabApplicationBuilder.configureJersey(this);
    }

    public NabApplicationBuilder bindToRoot() {
      return bindTo(ROOT_MAPPING);
    }

    private NabJerseyConfig prepareNabJerseyConfig() {
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


