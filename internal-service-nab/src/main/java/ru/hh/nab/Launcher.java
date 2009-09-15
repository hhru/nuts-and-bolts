package ru.hh.nab;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.guice.spi.container.GuiceComponentProviderFactory;
import com.sun.jersey.server.impl.container.grizzly.GrizzlyContainer;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.WebApplicationFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ServiceLoader;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class Launcher {
  public static void main(String[] args) {
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

  public static void main(Stage stage, Module appModule) {
    LogManager.getLogManager().reset();
    Logger rootLogger = LogManager.getLogManager().getLogger("");
    for (Handler h : rootLogger.getHandlers())
      rootLogger.removeHandler(h);
    SLF4JBridgeHandler.install();

    try {
      WebApplication wa = WebApplicationFactory.createWebApplication();
      Injector inj = Guice.createInjector(stage, appModule, new JerseyModule(wa),
              new SettingsModule());
      Settings settings = inj.getInstance(Settings.class);

      GrizzlyWebServer ws = new GrizzlyWebServer(settings.port);
      ws.setCoreThreads(settings.concurrencyLevel);
      ws.setMaxThreads(settings.concurrencyLevel);
      SelectorThread selector = ws.getSelectorThread();
      selector.setMaxKeepAliveRequests(4096);
      selector.setCompressionMinSize(Integer.MAX_VALUE);
      selector.setSendBufferSize(4096);
      selector.setBufferSize(4096);
      selector.setSelectorReadThreadsCount(1);
      selector.setUseDirectByteBuffer(true);
      selector.setUseByteBufferView(true);

      DefaultResourceConfig resources = new DefaultResourceConfig();
      wa.initiate(resources, new GuiceComponentProviderFactory(resources, inj));
      GrizzlyContainer jersey = new GrizzlyContainer(wa);
      jersey.setHandleStaticResources(false);
      jersey.setRootFolder(null);
      ws.addGrizzlyAdapter(jersey, new String[]{"/*"});
      ws.start();
    } catch (IOException ex) {
      Logger.getAnonymousLogger().log(Level.SEVERE, "boom", ex);
    }
  }
}
