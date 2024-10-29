package ru.hh.nab.starter;

import java.util.function.Consumer;

public final class NabApplicationBuilder {

  NabApplicationBuilder() {
  }

  // method for chaning
  public NabApplicationBuilder apply(Consumer<NabApplicationBuilder> operation) {
    operation.accept(this);
    return this;
  }

  public NabApplication build() {
    return new NabApplication(new NabServletContextConfig());
  }
}
