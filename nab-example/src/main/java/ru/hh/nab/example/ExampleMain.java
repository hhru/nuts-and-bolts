package ru.hh.nab.example;

import java.util.Set;
import org.testcontainers.containers.GenericContainer;
import ru.hh.nab.common.properties.PropertiesUtils;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.starter.NabProdConfig;
import ru.hh.nab.websocket.NabWebsocketConfigurator;

public class ExampleMain {

  public static void main(String[] args) {
    // specify settings dir if its not currentDir
    System.setProperty(PropertiesUtils.SETINGS_DIR_PROPERTY, "nab-example/src/etc");

    // you need to run consul agent to be able to run NaB application if you don't want to use consul add consul.enabled=false in config
    GenericContainer<?> consulContainer = new GenericContainer<>("consul")
        .withCommand("agent", "-dev", "-client", "0.0.0.0", "--enable-script-checks=true")
        .withExposedPorts(8500);

    consulContainer.start();
    int consulPort = consulContainer.getFirstMappedPort();
    // better to use settings with fixed port, but for the sake of dynamic usage we use env
    System.setProperty(NabProdConfig.CONSUL_PORT_PROPERTY, String.valueOf(consulPort));
    NabApplication
        .builder()
        .configureJersey(ExampleJerseyConfig.class)
        .addAllowedPackages("ru.hh")
        .bindToRoot()
        .apply(builder -> NabWebsocketConfigurator.configureWebsocket(builder, Set.of("ru.hh")))
        .build()
        .run(ExampleConfig.class);
  }
}
