package ru.hh.nab.example;

import ru.hh.nab.starter.NabApplication;

public class ExampleMain {

  public static void main(String[] args) {
    NabApplication.builder()
        .configureJersey(ExampleJerseyConfig.class).bindToRoot()
        .configureWebsocket(EchoSocket.class)
        .build().run(ExampleConfig.class);
  }
}
