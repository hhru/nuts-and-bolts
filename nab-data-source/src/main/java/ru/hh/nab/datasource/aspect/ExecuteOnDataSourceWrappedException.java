package ru.hh.nab.datasource.aspect;

import static java.util.Objects.requireNonNull;

public final class ExecuteOnDataSourceWrappedException extends RuntimeException {
  public ExecuteOnDataSourceWrappedException(Throwable cause) {
    super("Checked exception from @ExecuteOnDataSource", requireNonNull(cause));
  }
}
