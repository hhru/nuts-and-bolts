package ru.hh.nab.starter;

import javax.servlet.ServletContextEvent;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.starter.server.jetty.JettyServerFactory;
import ru.hh.nab.starter.servlet.WebAppInitializer;

public final class NabApplicationContext extends AnnotationConfigWebApplicationContext {

  private final WebAppInitializer webAppInitializer;
  private JettyServer jettyServer;

  NabApplicationContext(NabServletContextConfig servletConfig, Class<?>... primarySources) {
    register(primarySources);
    registerShutdownHook();
    webAppInitializer = createWebAppInitializer(this, servletConfig);
  }

  public void startApplication() {
    //partial refresh to get beans required for jetty, but without state change
    ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
    prepareBeanFactory(beanFactory);
    postProcessBeanFactory(beanFactory);
    invokeBeanFactoryPostProcessors(beanFactory);

    startJettyServer();
  }

  private void startJettyServer() {
    try {
      if (jettyServer == null) {
        final FileSettings jettySettings = getBean(FileSettings.class);
        final ThreadPool threadPool = getBean(ThreadPool.class);
        this.jettyServer = JettyServerFactory.create(jettySettings, threadPool, webAppInitializer);
        this.jettyServer.start();
      }
    } catch (Throwable t) {
      throw new ApplicationContextException("Unable to start application server", t);
    }
  }

  static WebAppInitializer createWebAppInitializer(WebApplicationContext ctx, NabServletContextConfig servletContextConfig) {
    return webApp -> {
      servletContextConfig.preConfigureWebApp(webApp, ctx);
      webApp.addEventListener(new ContextLoaderListener(ctx) {
        @Override
        public void contextInitialized(ServletContextEvent event) {
          super.contextInitialized(event);
          servletContextConfig.onWebAppStarted(event.getServletContext(), ctx);
          servletContextConfig.getListeners(ctx).forEach(listener -> listener.contextInitialized(event));
        }

        @Override
        public void contextDestroyed(ServletContextEvent event) {
          super.contextDestroyed(event);
          servletContextConfig.getListeners(ctx).forEach(listener -> listener.contextDestroyed(event));
        }
      });
    };
  }

  public boolean isServerRunning() {
    return jettyServer.isRunning();
  }
}
