package ru.hh.nab.example;

import ru.hh.nab.NabModule;

public class AppModule extends NabModule{
  @Override
  protected void configureApp() {
    bind(ExampleRs.class);
  }
}
