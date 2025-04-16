package ru.hh.nab.telemetry.jdbc.internal.model;

import io.opentelemetry.api.common.AttributeKey;
import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import jakarta.annotation.Nullable;
import java.util.Optional;
import javax.sql.DataSource;

public class NabDataSourceInfo {

  public static final AttributeKey<String> DATASOURCE_NAME_ATTRIBUTE_KEY = stringKey("datasource.name");
  public static final AttributeKey<Boolean> DATASOURCE_WRITABLE_ATTRIBUTE_KEY = booleanKey("datasource.writable");

  private DataSource dataSource;
  @Nullable
  private String dataSourceName;
  @Nullable
  private Boolean writableDataSource;

  public DataSource getDataSource() {
    return dataSource;
  }

  public NabDataSourceInfo setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
    return this;
  }

  public Optional<String> getDataSourceName() {
    return Optional.ofNullable(dataSourceName);
  }

  public NabDataSourceInfo setDataSourceName(@Nullable String dataSourceName) {
    this.dataSourceName = dataSourceName;
    return this;
  }

  public Optional<Boolean> isWritableDataSource() {
    return Optional.ofNullable(writableDataSource);
  }

  public NabDataSourceInfo setWritableDataSource(@Nullable Boolean writableDataSource) {
    this.writableDataSource = writableDataSource;
    return this;
  }
}
