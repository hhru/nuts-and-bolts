package ru.hh.nab.datasource.postgres.embedded;

import com.mchange.v2.c3p0.DataSources;
import com.mchange.v2.c3p0.DriverManagerDataSource;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import ru.hh.nab.common.util.FileSystemUtils;
import ru.hh.nab.datasource.jdbc.StatementTimeoutDataSource;

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

  private static final String EMBEDDED_PG_DIR = "embedded-pg";
  private static final String EMBEDDED_PG_DIR_PROPERTY = "ot.epg.working-dir";
  private static final String DEFAULT_JDBC_URL = "jdbc:postgresql://localhost:%s/postgres";

  private static final UUID instanceId = UUID.randomUUID();

  private EmbeddedPostgresDataSourceFactory() {
  }

  public static DataSource create() throws SQLException {
    return create(DEFAULT_JDBC_URL);
  }

  public static DataSource create(String jdbcUrl) throws SQLException {
    EmbeddedPostgres pgInstance = createEmbeddedPostgresInstance();

    DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource(false);
    driverManagerDataSource.setJdbcUrl(String.format(jdbcUrl, pgInstance.getPort()));
    driverManagerDataSource.setUser("postgres");

    DataSource statementTimeoutDataSource = new StatementTimeoutDataSource(driverManagerDataSource, 5000);

    Properties pooledProps = new Properties();
    pooledProps.setProperty("acquireIncrement", "1");
    return DataSources.pooledDataSource(statementTimeoutDataSource, pooledProps);
  }

  private static EmbeddedPostgres createEmbeddedPostgresInstance() {
    try {
      File dataDirectory = null;
      String embeddedPgDir = getEmbeddedPgDir();
      if (embeddedPgDir != null) {
        System.setProperty(EMBEDDED_PG_DIR_PROPERTY, embeddedPgDir);
        dataDirectory = new File(embeddedPgDir, instanceId.toString());
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

    Path pgPath = Paths.get(tmpfsPath.toString(), EMBEDDED_PG_DIR);
    if (Files.notExists(pgPath)) {
      Files.createDirectory(pgPath);
    }
    return pgPath.toString();
  }
}
