package ru.hh.nab.datasource;

import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Wrapper for DataSource which holds unique {@link #getName() name}.
 */
public class NamedDataSource extends DelegatingDataSource {

  public static Optional<String> getName(DataSource dataSource) {
    try {
      return Optional.of(dataSource.unwrap(NamedDataSource.class).getName());
    } catch (SQLException e) {
      return Optional.empty();
    }
  }

  private final String name;

  public NamedDataSource(String name, DataSource delegate) {
    super(delegate);
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
