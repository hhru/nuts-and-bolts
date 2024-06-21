package ru.hh.nab.hibernate.transaction;

import static java.util.Objects.requireNonNull;

final class ExecuteOnDataSourceWrappedException extends RuntimeException {
  public ExecuteOnDataSourceWrappedException(Throwable cause) {
    super("Checked exception from @ExecuteOnDataSource", requireNonNull(cause));
  }
}
