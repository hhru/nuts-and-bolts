package ru.hh.nab;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.GuiceComponentProviderFactory;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.WebApplicationFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;
import ru.hh.nab.NabModule.ServletDef;
import ru.hh.nab.NabModule.ServletDefs;
import ru.hh.nab.jersey.HeadersAnnotationFilterFactory;
import ru.hh.nab.jersey.NabGrizzlyContainer;

public class Launcher {
  static NabModule module;

  public static void main(String[] args) throws IOException {
    ArrayList<NabModule> modules = Lists.newArrayList(ServiceLoader.load(NabModule.class).iterator());
    if (modules.size() == 0)
      throw new IllegalStateException("No instances of " + NabModule.class.getName() + " found");
    if (modules.size() > 1)
      throw new IllegalStateException(
              "Two or more instances of " + NabModule.class.getName() + " found: " + Joiner.on(", ")
                      .join(Iterables.transform(modules, new Function<NabModule, Object>() {
                        @Override
                        public Object apply(NabModule from) {
                          return from.getClass().getName();
                        }
                      })));
    main(Stage.PRODUCTION, Iterables.getOnlyElement(modules));
  }

  public static void main(Stage stage, NabModule appModule) throws IOException {
    main(stage, appModule, new SettingsModule());
  }

  public static Instance testMode(Stage stage, NabModule appModule, final Properties settings) throws IOException {
    return main(stage, appModule, new AbstractModule() {
      public void configure() {}

      @Provides
      @Singleton
      protected Settings settings() throws IOException {
        settings.setProperty("port", "0");
        return new Settings(settings);
      }
    });
  }

  public static Instance main(Stage stage, NabModule appModule, Module settingsModule) throws IOException {
    module = appModule;
    LogManager.getLogManager().reset();
    Logger rootLogger = LogManager.getLogManager().getLogger("");
    for (Handler h : rootLogger.getHandlers())
      rootLogger.removeHandler(h);
    SLF4JBridgeHandler.install();

    String settingsDir = System.getProperty("settingsDir");
    if (settingsDir != null) {
      LogManager.getLogManager().readConfiguration(
              new FileInputStream(new File(settingsDir, "logback.xml")));
    }

    try {
      WebApplication wa = WebApplicationFactory.createWebApplication();
      Injector inj = Guice.createInjector(stage, new JerseyGutsModule(wa), appModule,
              new JerseyModule(wa), settingsModule);

      GrizzlyWebServer ws = inj.getInstance(GrizzlyWebServer.class);
      ws.start();
      return new Instance(inj, ws.getSelectorThread().getPort());
    } catch (IOException ex) {
      Logger.getAnonymousLogger().log(Level.SEVERE, "boom", ex);
      throw new RuntimeException(ex);
    }
  }

  public static class Instance {
    public final Injector injector;
    public final int port;

    public Instance(Injector injector, int port) {
      this.injector = injector;
      this.port = port;
    }
  }
}
