package ru.hh.nab.starter;

import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toMap;

import io.sentry.Sentry;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import javax.servlet.ServletContextEvent;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ru.hh.nab.common.properties.FileSettings;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.starter.server.jetty.JettyServerFactory;
import ru.hh.nab.starter.servlet.WebAppInitializer;

public final class NabApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(NabApplication.class);

  private final NabServletContextConfig servletContextConfig;

  NabApplication(NabServletContextConfig servletContextConfig) {
    this.servletContextConfig = servletContextConfig;
  }

  public static JettyServer runDefaultWebApp(Class<?>... configurationClasses) {
    return runWebApp(new NabServletContextConfig(), configurationClasses);
  }

  public static JettyServer runWebApp(NabServletContextConfig servletContextConfig, Class<?>... configurationClasses) {
    return new NabApplication(servletContextConfig).run(configurationClasses);
  }

  public JettyServer run(Class<?>... configs) {
    AnnotationConfigWebApplicationContext aggregateCtx = new AnnotationConfigWebApplicationContext();
    aggregateCtx.register(configs);
    aggregateCtx.refresh();
    return run(aggregateCtx);
  }

  public JettyServer run(WebApplicationContext baseContext) {
    return run(baseContext, false);
  }

  /**
   *
   * @param directlyUseAsWebAppRoot if this context used directly it gets no initialization by initializing listener
   * {@link ContextLoader#configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext, javax.servlet.ServletContext)}
   */
  public JettyServer run(WebApplicationContext baseContext, boolean directlyUseAsWebAppRoot) {
    configureLogger();
    try {
      FileSettings fileSettings = baseContext.getBean(FileSettings.class);
      ThreadPool threadPool = baseContext.getBean(ThreadPool.class);
      configureSentry(baseContext);
      JettyServer jettyServer = JettyServerFactory.create(fileSettings, threadPool,
        createWebAppInitializer(servletContextConfig, baseContext, directlyUseAsWebAppRoot));
      jettyServer.start();
      logStartupInfo(baseContext);
      return jettyServer;
    } catch (Exception e) {
      return logErrorAndExit(e);
    }
  }

  public static NabApplicationBuilder builder() {
    return new NabApplicationBuilder();
  }

  public static void configureLogger() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  public static void configureSentry(ApplicationContext context) {
    FileSettings settings = context.getBean(FileSettings.class);
    Sentry.init(settings.getString("sentry.dsn"));
  }

  private static void logStartupInfo(ApplicationContext context) {
    AppMetadata appMetadata = context.getBean(AppMetadata.class);
    LOGGER.info("Started {} PID={} (version={})", appMetadata.getServiceName(), getCurrentPid(), appMetadata.getVersion());
  }

  private static String getCurrentPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  private static <T> T logErrorAndExit(Exception e) {
    try {
      LOGGER.error("Failed to start, shutting down", e);
      System.err.println(format("[{0}] Failed to start, shutting down: {1}", LocalDateTime.now(), e.getMessage()));
      return null;
    } finally {
      System.exit(1);
    }
  }

  private static WebAppInitializer createWebAppInitializer(NabServletContextConfig servletContextConfig, WebApplicationContext baseCtx,
      boolean directWebappRoot) {
    WebApplicationContext targetCtx = directWebappRoot ? baseCtx : createChildWebAppCtx(baseCtx);
    return webApp -> {
      servletContextConfig.preConfigureWebApp(webApp, baseCtx);
      webApp.addEventListener(new ContextLoaderListener(targetCtx) {
        @Override
        public void contextInitialized(ServletContextEvent event) {
          super.contextInitialized(event);
          servletContextConfig.onWebAppStarted(event.getServletContext(), targetCtx);
          servletContextConfig.getListeners(targetCtx).forEach(listener -> listener.contextInitialized(event));
        }

        @Override
        public void contextDestroyed(ServletContextEvent event) {
          super.contextDestroyed(event);
          servletContextConfig.getListeners(targetCtx).forEach(listener -> listener.contextDestroyed(event));
          if (baseCtx instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) baseCtx).close();
          }
        }
      });
    };
  }

  private static AnnotationConfigWebApplicationContext createChildWebAppCtx(WebApplicationContext baseCtx) {
    AnnotationConfigWebApplicationContext webAppRootCtx = new HierarchialWebApplicationContext(baseCtx);
    webAppRootCtx.setParent(baseCtx);
    webAppRootCtx.registerShutdownHook();
    if (baseCtx.getServletContext() != null) {
      webAppRootCtx.setServletContext(baseCtx.getServletContext());
    }
    return webAppRootCtx;
  }

  private static final class HierarchialWebApplicationContext extends AnnotationConfigWebApplicationContext {
    private HierarchialWebApplicationContext(WebApplicationContext parentCtx) {
      setParent(parentCtx);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
      return BeanFactoryUtils.beansOfTypeIncludingAncestors(getBeanFactory(), type);
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
      return Arrays.stream(BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(getBeanFactory(), annotationType))
        .collect(toMap(Function.identity(), this::getBean));
    }
  }
}
