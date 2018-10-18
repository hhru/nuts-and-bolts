package ru.hh.nab.starter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextListener;
import ru.hh.nab.starter.filters.RequestIdLoggingFilter;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import ru.hh.nab.starter.jersey.DefaultResourceConfig;
import ru.hh.nab.starter.resource.StatsResource;
import ru.hh.nab.starter.resource.StatusResource;
import ru.hh.nab.starter.servlet.NabJerseyConfig;
import ru.hh.nab.starter.servlet.NabServletConfig;

public class NabServletContextConfig {

  protected List<ServletContextListener> getListeners(WebApplicationContext rootCtx) {
    return Collections.emptyList();
  }

  protected List<NabServletConfig> getServletConfigs(WebApplicationContext rootCtx) {
    return Collections.emptyList();
  }

  protected NabJerseyConfig getJerseyConfig() {
    return NabJerseyConfig.NONE;
  }

  /**
   * should be called before server doStart() is called
   * Can be used to preconfigure things that required to be inited. Ex: for class-based FilterHolder
   * org.eclipse.jetty.servlet.FilterHolder#initialize() will be called by the container later
   */
  void preConfigureWebApp(ServletContextHandler servletContextHandler, WebApplicationContext rootCtx) {
    configureServletContextHandler(servletContextHandler, rootCtx);
  }

  protected void configureServletContextHandler(ServletContextHandler servletContextHandler, WebApplicationContext rootCtx) {
    setPaths(servletContextHandler);
  }

  private void setPaths(ServletContextHandler servletContextHandler) {
    servletContextHandler.setContextPath(getContextPath());
    servletContextHandler.setResourceBase(getResourceBase());
  }

  protected String getResourceBase() {
    return "src/main/webapp";
  }

  protected String getContextPath() {
    return "/";
  }

  /**
   * should be called after server doStart() is called
   * all static init() methods already called here. So for class-based FilterHolder
   * org.eclipse.jetty.servlet.FilterHolder#initialize() won't be called anymore
   */
  void onWebAppStarted(ServletContext servletContext, WebApplicationContext rootCtx) {
    configureServletContext(servletContext, rootCtx);
    registerServlets(servletContext, rootCtx, getServletConfigs(rootCtx));
  }

  protected void configureServletContext(ServletContext servletContext, WebApplicationContext rootCtx) {
    servletContext.addListener(new RequestContextListener());
    registerFilter(servletContext, RequestIdLoggingFilter.class.getName(), RequestIdLoggingFilter.class, Collections.emptyMap(),
      EnumSet.allOf(DispatcherType.class), "/*");
    if (rootCtx.containsBean("cacheFilter")) {
      FilterHolder cacheFilter = rootCtx.getBean("cacheFilter", FilterHolder.class);
      if (cacheFilter.isInstance()) {
        registerFilter(servletContext, cacheFilter.getName(), cacheFilter, EnumSet.allOf(DispatcherType.class), "/*");
      }
    }
  }

  private void registerServlets(ServletContext servletContext, WebApplicationContext rootCtx, List<NabServletConfig> servletConfigs) {
    registerStatusServlets(servletContext, rootCtx);
    registerJersey(servletContext, rootCtx);
    servletConfigs.forEach(nabServletConfig -> {
      Servlet servlet = nabServletConfig.createServlet(rootCtx);
      if (servlet instanceof ServletContainer) {
        throw new IllegalArgumentException("Please register Jersey servlets via NabJerseyConfig");
      }
      registerServlet(servletContext, servlet, nabServletConfig.getName(), nabServletConfig.getMapping());
    });
  }

  private void registerJersey(ServletContext servletContext, WebApplicationContext rootCtx) {
    NabJerseyConfig nabJerseyConfig = getJerseyConfig();
    if (nabJerseyConfig.isDisabled()) {
      return;
    }
    ResourceConfig resourceConfig = createResourceConfig(rootCtx, nabJerseyConfig.getAllowedPackages());
    nabJerseyConfig.configure(resourceConfig);
    Servlet servletContainer = new ServletContainer(resourceConfig);
    registerServlet(servletContext, servletContainer, nabJerseyConfig.getName(), nabJerseyConfig.getMapping());
  }

  private static ResourceConfig createResourceConfig(WebApplicationContext ctx, List<String> allowedPackages) {
    ResourceConfig resourceConfig = new DefaultResourceConfig();
    ctx.getBeansWithAnnotation(javax.ws.rs.Path.class).entrySet().stream()
      .filter(e -> allowedPackages.stream().anyMatch(allowedPackage -> e.getValue().getClass().getName().startsWith(allowedPackage)))
      .map(Map.Entry::getValue).forEach(resourceConfig::register);
    return resourceConfig;
  }

  private static void registerStatusServlets(ServletContext servletContext, WebApplicationContext rootCtx) {
    ResourceConfig statusResourceConfig = new ResourceConfig();
    statusResourceConfig.register(new StatusResource(rootCtx.getBean(AppMetadata.class)));
    Servlet statusServletContainer = new ServletContainer(statusResourceConfig);
    registerServlet(servletContext, statusServletContainer, "status", "/status");

    ResourceConfig statsResourceConfig = new ResourceConfig();
    statsResourceConfig.register(new StatsResource());
    Servlet statsServletContainer = new ServletContainer(statsResourceConfig);
    registerServlet(servletContext, statsServletContainer, "stats", "/stats");
  }

  private static void registerServlet(ServletContext servletContext, Servlet servlet, String servletName, String... mappings) {
    validateMappings(mappings);
    ServletRegistration.Dynamic dynamic = servletContext.addServlet(servletName, servlet);
    dynamic.addMapping(mappings);
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
    if (filterHolder.getFilter() != null) {
      registerFilter(servletContext, filterName, filterHolder.getFilter(), dispatcherTypes, mappings);
    } else {
      registerFilter(servletContext, filterName, filterHolder.getHeldClass(), filterHolder.getInitParameters(),
        dispatcherTypes, mappings);
    }
  }

  public TestBridge getPublicMorozovBridge() {
    return new TestBridge();
  }

  public final class TestBridge {

    private TestBridge() {
    }

    public void preConfigureWebApp(ServletContextHandler servletContextHandler, WebApplicationContext rootCtx) {
      NabServletContextConfig.this.preConfigureWebApp(servletContextHandler, rootCtx);
    }

    public void onWebAppStarted(ServletContext servletContext, WebApplicationContext rootCtx) {
      NabServletContextConfig.this.onWebAppStarted(servletContext, rootCtx);
    }
  }
}
