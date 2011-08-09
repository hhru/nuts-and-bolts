package ru.hh.nab.example;

import ru.hh.nab.NabModule;
import ru.ru.hh.nab.example.ExampleRs;

public class AppModule extends NabModule{
  @Override
  protected void configureApp() {
    bind(ExampleRs.class);
  }
}
