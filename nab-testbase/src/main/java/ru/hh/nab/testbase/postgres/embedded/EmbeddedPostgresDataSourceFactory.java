package ru.hh.nab.testbase.postgres.embedded;

import com.mchange.v2.c3p0.DataSources;
import com.mchange.v2.c3p0.DriverManagerDataSource;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import ru.hh.nab.common.files.FileSystemUtils;
import ru.hh.nab.datasource.monitoring.StatementTimeoutDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

public class EmbeddedPostgresDataSourceFactory {
  public static final String DEFAULT_JDBC_URL = "jdbc:postgresql://localhost:%s/postgres";
  public static final String DEFAULT_USER = "postgres";

  private static final String PG_DIR = "embedded-pg";
  private static final String PG_DIR_PROPERTY = "ot.epg.working-dir";
  private static final UUID INSTANCE_ID = UUID.randomUUID();

  public static DataSource create() throws SQLException {
    return create(DEFAULT_JDBC_URL);
  }

  public static DataSource create(String jdbcUrl) throws SQLException {
    DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource(false);
    driverManagerDataSource.setJdbcUrl(String.format(jdbcUrl, getEmbeddedPostgres().getPort()));
    driverManagerDataSource.setUser(DEFAULT_USER);

    DataSource statementTimeoutDataSource = new StatementTimeoutDataSource(driverManagerDataSource, 5000);

    Properties poolProperties = new Properties();
    poolProperties.setProperty("acquireIncrement", "1");
    return DataSources.pooledDataSource(statementTimeoutDataSource, poolProperties);
  }

  private static EmbeddedPostgres createEmbeddedPostgres() {
    try {
      File dataDirectory = null;
      String embeddedPgDir = getEmbeddedPgDir();
      if (embeddedPgDir != null) {
        System.setProperty(PG_DIR_PROPERTY, embeddedPgDir);
        dataDirectory = new File(embeddedPgDir, INSTANCE_ID.toString());
      }
      return EmbeddedPostgres.builder()
          .setServerConfig("autovacuum", "off")
          .setLocaleConfig("lc-collate", "C")
          .setDataDirectory(dataDirectory)
          .start();
    } catch (IOException e) {
      throw new IllegalStateException("Can't start embedded Postgres", e);
    }
  }

  private static String getEmbeddedPgDir() throws IOException {
    Path tmpfsPath = FileSystemUtils.getTmpfsPath();
    if (tmpfsPath == null) {
      return null;
    }

    Path pgPath = Paths.get(tmpfsPath.toString(), PG_DIR);
    if (Files.notExists(pgPath)) {
      Files.createDirectory(pgPath);
    }
    return pgPath.toString();
  }

  private static class EmbeddedPostgresSingleton {
    private static final EmbeddedPostgres INSTANCE = createEmbeddedPostgres();
  }

  public static EmbeddedPostgres getEmbeddedPostgres() {
    return EmbeddedPostgresSingleton.INSTANCE;
  }

  private EmbeddedPostgresDataSourceFactory() {
  }
}
