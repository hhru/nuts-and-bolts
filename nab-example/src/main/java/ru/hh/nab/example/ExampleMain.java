package ru.hh.nab.example;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.hh.nab.core.Launcher;

public class ExampleMain extends Launcher {

  public static void main(String[] args) {
    doMain(new AnnotationConfigApplicationContext(ExampleProdConfig.class));
  }
}
