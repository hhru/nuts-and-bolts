package ru.hh.nab.starter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.starter.servlet.NabJerseyConfig;
import ru.hh.nab.starter.servlet.NabServletConfig;

public final class NabApplicationBuilder {
  public static final String[] ROOT_MAPPING = {"/*"};

  private final List<NabServletConfig.Builder> servletBuilders;
  private final List<Function<WebApplicationContext, ServletContextListener>> listenerProviders;
  private final List<BiConsumer<ServletContext, WebApplicationContext>> servletContextConfigurers;
  private final List<BiConsumer<WebAppContext, WebApplicationContext>> servletContextHandlerConfigurers;

  private NabJerseyConfig.Builder jerseyBuilder;
  private String contextPath;
  private ClassLoader classLoader;

  NabApplicationBuilder() {
    servletBuilders = new ArrayList<>();
    listenerProviders = new ArrayList<>();
    servletContextConfigurers = new ArrayList<>();
    servletContextHandlerConfigurers = new ArrayList<>();
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
        servletContextHandlerConfigurers.forEach(cfg -> cfg.accept(webAppContext, rootCtx));
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
        return servletBuilders.stream().map(NabServletConfig.Builder::build).collect(Collectors.toList());
      }

      @Override
      protected NabJerseyConfig getJerseyConfig() {
        if (jerseyBuilder == null) {
          return super.getJerseyConfig();
        }
        return jerseyBuilder.build();
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
    this.servletContextHandlerConfigurers.add(servletContextHandlerConfigurer);
    return this;
  }

  public NabServletConfig.Builder addServlet(Function<WebApplicationContext, Servlet> servletInitializer, Class<?>... childConfigs) {
    return new NabServletConfig.Builder(this, servletInitializer, childConfigs);
  }

  public NabJerseyConfig.Builder configureJersey(Class<?>... childConfigs) {
    return new NabJerseyConfig.Builder(this, childConfigs);
  }

  public NabApplicationBuilder addServlet(NabServletConfig.Builder servletBuilder) {
    servletBuilders.add(servletBuilder);
    return this;
  }

  public NabApplicationBuilder configureJersey(NabJerseyConfig.Builder jerseyBuilder) {
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
      return bindTo(ROOT_MAPPING);
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

}
