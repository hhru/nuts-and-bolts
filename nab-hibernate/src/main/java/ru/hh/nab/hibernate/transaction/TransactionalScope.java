package ru.hh.nab.hibernate.transaction;

import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalScope {

  @Transactional(readOnly = true)
  public <T> T read(Supplier<T> supplier) {
    return supplier.get();
  }

  @Transactional(readOnly = true)
  public void read(Runnable runnable) {
    runnable.run();
  }

  @Transactional
  public <T> T write(Supplier<T> supplier) {
    return supplier.get();
  }

  @Transactional
  public void write(Runnable runnable) {
    runnable.run();
  }

}
