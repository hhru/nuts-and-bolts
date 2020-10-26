package ru.hh.nab.example;

import java.util.Set;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.websocket.NabWebsocketConfigurator;

public class ExampleMain {

  public static void main(String[] args) {
    NabApplication.builder()
        .configureJersey(ExampleJerseyConfig.class).bindToRoot()
        .apply(builder -> NabWebsocketConfigurator.configureWebsocket(builder, Set.of("ru.hh")))
        .build().run(ExampleConfig.class);
  }
}
