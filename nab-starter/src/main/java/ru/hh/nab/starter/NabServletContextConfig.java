package ru.hh.nab.starter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextListener;
import ru.hh.nab.starter.filters.RequestIdLoggingFilter;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import ru.hh.nab.starter.resource.StatusResource;
import ru.hh.nab.starter.servlet.NabJerseyConfig;
import ru.hh.nab.starter.servlet.NabServletConfig;

public class NabServletContextConfig {

  public static final String[] DEFAULT_MAPPING = {"/*"};


  protected List<ServletContextListener> getListeners(WebApplicationContext rootCtx) {
    return Collections.emptyList();
  }

  protected List<NabServletConfig> getServletConfigs(WebApplicationContext rootCtx) {
    return Collections.emptyList();
  }

  protected NabJerseyConfig getJerseyConfig() {
    return NabJerseyConfig.DISABLED;
  }

  /**
   * should be called before server doStart() is called
   * Can be used to preconfigure things that required to be inited. Ex: for class-based FilterHolder
   * org.eclipse.jetty.servlet.FilterHolder#initialize() will be called by the container later
   */
  void preConfigureWebApp(WebAppContext webAppContext, WebApplicationContext rootCtx) {
    webAppContext.setContextPath(getContextPath());
    webAppContext.setClassLoader(getClassLoader());
    registerFilter(webAppContext.getServletContext(), RequestIdLoggingFilter.class.getName(), RequestIdLoggingFilter.class,
      Collections.emptyMap(), EnumSet.allOf(DispatcherType.class), DEFAULT_MAPPING);
    if (rootCtx.containsBean("cacheFilter")) {
      FilterHolder cacheFilter = rootCtx.getBean("cacheFilter", FilterHolder.class);
      if (cacheFilter.isInstance()) {
        registerFilter(webAppContext.getServletContext(), cacheFilter.getName(), cacheFilter,
          EnumSet.allOf(DispatcherType.class), DEFAULT_MAPPING);
      }
    }
    configureWebapp(webAppContext, rootCtx);
  }

  protected void configureWebapp(WebAppContext webAppContext, WebApplicationContext rootCtx) { }

  protected String getContextPath() {
    return "/";
  }

  protected ClassLoader getClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  /**
   * should be called after server doStart() is called
   * all static init() methods already called here. So for class-based FilterHolder
   * org.eclipse.jetty.servlet.FilterHolder#initialize() won't be called anymore
   */
  void onWebAppStarted(ServletContext servletContext, WebApplicationContext rootCtx) {
    servletContext.addListener(new RequestContextListener());
    configureServletContext(servletContext, rootCtx);
    List<NabServletConfig> servletConfigs = compileFullServletConfiguration(rootCtx);
    registerServlets(servletConfigs, servletContext, rootCtx);
  }

  private List<NabServletConfig> compileFullServletConfiguration(WebApplicationContext rootCtx) {
    List<NabServletConfig> servletConfigs = getServletConfigs(rootCtx);
    servletConfigs.forEach(servlet -> {
      if (servlet instanceof ServletContainer) {
        throw new IllegalArgumentException("Please register Jersey servlets via NabJerseyConfig");
      }
    });
    servletConfigs = new ArrayList<>(servletConfigs);
    servletConfigs.add(getJerseyConfig());
    servletConfigs.add(0, createStatusServletConfig());
    return Collections.unmodifiableList(servletConfigs);
  }

  protected void configureServletContext(ServletContext servletContext, WebApplicationContext rootCtx) { }

  private static void registerServlets(List<NabServletConfig> servletConfigs, ServletContext servletContext, WebApplicationContext rootCtx) {
    servletConfigs.stream()
      .filter(servletConfig -> !servletConfig.isDisabled())
      .forEach(nabServletConfig -> registerServlet(nabServletConfig, servletContext, rootCtx));
  }

  private static void registerServlet(NabServletConfig nabServletConfig, ServletContext servletContext, WebApplicationContext rootCtx) {
    validateMappings(nabServletConfig.getMapping());

    Servlet servlet = nabServletConfig.createServlet(rootCtx);
    ServletRegistration.Dynamic dynamic = servletContext.addServlet(nabServletConfig.getName(), servlet);
    Set<String> mappingConflicts = dynamic.addMapping(nabServletConfig.getMapping());
    if (!mappingConflicts.isEmpty()) {
      throw new RuntimeException("Servlet [" + nabServletConfig.getName() + "] has conflicting mappings: " + String.join(",", mappingConflicts));
    }
    dynamic.setInitParameters(nabServletConfig.getInitParameters());
    dynamic.setAsyncSupported(Boolean.parseBoolean(nabServletConfig.getInitParameters().getOrDefault("async-supported", "true")));
  }

  private static void validateMappings(String[] mappings) {
    if (mappings == null || mappings.length == 0) {
      throw new IllegalArgumentException("URL mapping must be present");
    }
  }

  public static <F extends Filter> void registerFilter(ServletContext servletContext, String filterName, Class<F> filterClass,
    Map<String, String> initParameters, EnumSet<DispatcherType> dispatcherTypes, String... mappings) {
    validateMappings(mappings);

    FilterRegistration.Dynamic dynamic = servletContext.addFilter(filterName, filterClass);
    dynamic.setInitParameters(initParameters);
    dynamic.addMappingForUrlPatterns(dispatcherTypes, true, mappings);
    dynamic.setAsyncSupported(Boolean.parseBoolean(initParameters.getOrDefault("async-supported", "true")));
  }

  public static <F extends Filter> void registerFilter(ServletContext servletContext, String filterName, F filter,
    EnumSet<DispatcherType> dispatcherTypes, String... mappings) {
    validateMappings(mappings);
    FilterRegistration.Dynamic dynamic = servletContext.addFilter(filterName, filter);
    dynamic.addMappingForUrlPatterns(dispatcherTypes, true, mappings);
  }

  public static void registerFilter(ServletContext servletContext, String filterName, FilterHolder filterHolder,
      EnumSet<DispatcherType> dispatcherTypes, String... mappings) {
    validateMappings(mappings);
    if (filterHolder.isInstance()) {
      registerFilter(servletContext, filterName, filterHolder.getFilter(), dispatcherTypes, mappings);
    } else {
      registerFilter(servletContext, filterName, filterHolder.getHeldClass(), filterHolder.getInitParameters(),
        dispatcherTypes, mappings);
    }
  }

  private static NabServletConfig createStatusServletConfig() {
    return new NabServletConfig() {
      @Override
      public String[] getMapping() {
        return new String[] {"/status"};
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
    };
  }
}
