package ru.hh.nab.example;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.hh.nab.NabLauncher;

public class ExampleMain extends NabLauncher {

  public static void main(String[] args) {
    doMain(new AnnotationConfigApplicationContext(ExampleProdConfig.class));
  }
}
