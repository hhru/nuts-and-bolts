package ru.hh.nab.starter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextListener;
import ru.hh.nab.common.component.NabServletFilter;
import ru.hh.nab.starter.filters.CommonHeadersFilter;
import ru.hh.nab.starter.filters.RequestIdLoggingFilter;
import ru.hh.nab.starter.filters.SentryFilter;
import ru.hh.nab.starter.servlet.NabJerseyConfig;
import ru.hh.nab.starter.servlet.NabServletConfig;
import ru.hh.nab.starter.servlet.StatusServletConfig;

public class NabServletContextConfig {

  public static final String[] DEFAULT_MAPPING = {"/*"};
  public static final String[] DEFAULT_SERVLET_NAMES = {};

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
    webAppContext.addEventListener(new RequestContextListener());
    registerFilter(
        webAppContext.getServletContext(),
        RequestIdLoggingFilter.class.getName(),
        RequestIdLoggingFilter.class,
        Collections.emptyMap(),
        EnumSet.allOf(DispatcherType.class),
        DEFAULT_MAPPING,
        DEFAULT_SERVLET_NAMES
    );
    registerFilter(
        webAppContext.getServletContext(),
        SentryFilter.class.getName(),
        SentryFilter.class,
        Collections.emptyMap(),
        EnumSet.allOf(DispatcherType.class),
        DEFAULT_MAPPING,
        DEFAULT_SERVLET_NAMES
    );
    registerFilter(
        webAppContext.getServletContext(),
        CommonHeadersFilter.class.getName(),
        CommonHeadersFilter.class,
        Collections.emptyMap(),
        EnumSet.allOf(DispatcherType.class),
        DEFAULT_MAPPING,
        DEFAULT_SERVLET_NAMES
    );
    rootCtx
        .getBeansOfType(NabServletFilter.class)
        .entrySet()
        .stream()
        .map(entry -> Map.entry(entry.getKey(), (Filter) entry.getValue()))
        .forEach(entry -> registerFilter(
            webAppContext.getServletContext(),
            entry.getKey(),
            entry.getValue(),
            EnumSet.allOf(DispatcherType.class),
            DEFAULT_MAPPING,
            DEFAULT_SERVLET_NAMES
        ));
    configureWebapp(webAppContext, rootCtx);
  }

  protected void configureWebapp(WebAppContext webAppContext, WebApplicationContext rootCtx) {
  }

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
    servletConfigs.add(0, new StatusServletConfig());
    return Collections.unmodifiableList(servletConfigs);
  }

  protected void configureServletContext(ServletContext servletContext, WebApplicationContext rootCtx) {
  }

  private static void registerServlets(List<NabServletConfig> servletConfigs, ServletContext servletContext, WebApplicationContext rootCtx) {
    servletConfigs
        .stream()
        .filter(servletConfig -> !servletConfig.isDisabled())
        .forEach(nabServletConfig -> registerServlet(nabServletConfig, servletContext, rootCtx));
  }

  private static void registerServlet(NabServletConfig nabServletConfig, ServletContext servletContext, WebApplicationContext rootCtx) {
    validateServletMappings(nabServletConfig.getMapping());

    Servlet servlet = nabServletConfig.createServlet(rootCtx);
    ServletRegistration.Dynamic dynamic = servletContext.addServlet(nabServletConfig.getName(), servlet);
    Set<String> mappingConflicts = dynamic.addMapping(nabServletConfig.getMapping());
    if (!mappingConflicts.isEmpty()) {
      throw new RuntimeException("Servlet [" + nabServletConfig.getName() + "] has conflicting mappings: " + String.join(",", mappingConflicts));
    }
    dynamic.setInitParameters(nabServletConfig.getInitParameters());
    dynamic.setAsyncSupported(Boolean.parseBoolean(nabServletConfig.getInitParameters().getOrDefault("async-supported", "true")));
  }

  private static void validateServletMappings(String[] mappings) {
    if (ArrayUtils.isEmpty(mappings)) {
      throw new IllegalArgumentException("URL mapping must be present");
    }
  }

  private static void validateFilterMappingsAndServletNames(String[] mappings, String[] servletNames) {
    boolean mappingsAreEmpty = ArrayUtils.isEmpty(mappings);
    boolean servletNamesAreEmpty = ArrayUtils.isEmpty(servletNames);

    if (mappingsAreEmpty && servletNamesAreEmpty) {
      throw new IllegalArgumentException("URL mapping or servlet names must be present");
    }
    if (!mappingsAreEmpty && !servletNamesAreEmpty) {
      throw new IllegalArgumentException("URL mapping and servlet names are mutually exclusive");
    }
  }

  public static <F extends Filter> void registerFilter(
      ServletContext servletContext,
      String filterName,
      Class<F> filterClass,
      Map<String, String> initParameters,
      EnumSet<DispatcherType> dispatcherTypes,
      String[] mappings,
      String[] servletNames
  ) {
    validateFilterMappingsAndServletNames(mappings, servletNames);

    FilterRegistration.Dynamic dynamic = servletContext.addFilter(filterName, filterClass);
    dynamic.setInitParameters(initParameters);
    if (ArrayUtils.isEmpty(mappings)) {
      dynamic.addMappingForServletNames(dispatcherTypes, true, servletNames);
    } else {
      dynamic.addMappingForUrlPatterns(dispatcherTypes, true, mappings);
    }
    dynamic.setAsyncSupported(Boolean.parseBoolean(initParameters.getOrDefault("async-supported", "true")));
  }

  public static <F extends Filter> void registerFilter(
      ServletContext servletContext,
      String filterName,
      F filter,
      EnumSet<DispatcherType> dispatcherTypes,
      String[] mappings,
      String[] servletNames
  ) {
    validateFilterMappingsAndServletNames(mappings, servletNames);
    FilterRegistration.Dynamic dynamic = servletContext.addFilter(filterName, filter);
    dynamic.setAsyncSupported(true);
    if (ArrayUtils.isEmpty(mappings)) {
      dynamic.addMappingForServletNames(dispatcherTypes, true, servletNames);
    } else {
      dynamic.addMappingForUrlPatterns(dispatcherTypes, true, mappings);
    }
  }

  public static void registerFilter(
      ServletContext servletContext,
      String filterName,
      FilterHolder filterHolder,
      EnumSet<DispatcherType> dispatcherTypes,
      String[] mappings,
      String[] servletNames
  ) {
    validateFilterMappingsAndServletNames(mappings, servletNames);
    if (filterHolder.isInstance()) {
      registerFilter(servletContext, filterName, filterHolder.getFilter(), dispatcherTypes, mappings, servletNames);
    } else {
      registerFilter(
          servletContext,
          filterName,
          filterHolder.getHeldClass(),
          filterHolder.getInitParameters(),
          dispatcherTypes,
          mappings,
          servletNames
      );
    }
  }

}
