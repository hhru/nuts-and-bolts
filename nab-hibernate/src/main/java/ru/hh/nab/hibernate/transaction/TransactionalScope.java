package ru.hh.nab.hibernate.transaction;

import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalScope {

  @Transactional(value = "transactionManager", readOnly = true)
  public <T> T read(Supplier<T> supplier) {
    return supplier.get();
  }

  @Transactional(value = "transactionManager", readOnly = true)
  public void read(Runnable runnable) {
    runnable.run();
  }

  @Transactional(value = "transactionManager")
  public <T> T write(Supplier<T> supplier) {
    return supplier.get();
  }

  @Transactional(value = "transactionManager")
  public void write(Runnable runnable) {
    runnable.run();
  }

}
