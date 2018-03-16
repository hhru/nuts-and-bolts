package ru.hh.nab.datasource.postgres.embedded;

import com.mchange.v2.c3p0.DataSources;
import com.mchange.v2.c3p0.DriverManagerDataSource;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import ru.hh.nab.common.util.FileSystemUtils;
import ru.hh.nab.datasource.jdbc.StatementTimeoutDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Properties;

public class EmbeddedPostgresDataSourceFactory {

  private static final String EMBEDDED_PG_DIR = "embedded-pg";
  private static final String EMBEDDED_PG_DIR_PROPERTY = "ness.embedded-pg.dir";

  public static DataSource create() throws SQLException {
    EmbeddedPostgres pgInstance = createEmbeddedPostgresInstance();

    DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource(false);
    driverManagerDataSource.setJdbcUrl("jdbc:postgresql://localhost:" + pgInstance.getPort() + "/postgres");
    driverManagerDataSource.setUser("postgres");

    DataSource statementTimeoutDataSource = new StatementTimeoutDataSource(driverManagerDataSource, 5000);

    Properties pooledProps = new Properties();
    pooledProps.setProperty("acquireIncrement", "1");
    return DataSources.pooledDataSource(statementTimeoutDataSource, pooledProps);
  }

  private static EmbeddedPostgres createEmbeddedPostgresInstance() {
    try {
      setEmbeddedPgDir();
      return EmbeddedPostgres.builder()
          .setServerConfig("autovacuum", "off")
          .setLocaleConfig("lc-collate", "C")
          .start();
    } catch (IOException e) {
      throw new IllegalStateException("Can't start embedded Postgres", e);
    }
  }

  private static void setEmbeddedPgDir() throws IOException {
    Path tmpfsPath = FileSystemUtils.getTmpfsPath();
    if (tmpfsPath != null) {
      Path pgPath = Paths.get(tmpfsPath.toString(), EMBEDDED_PG_DIR);
      if (Files.notExists(pgPath)) {
        Files.createDirectory(pgPath);
      }
      System.setProperty(EMBEDDED_PG_DIR_PROPERTY, pgPath.toString());
    }
  }

  private EmbeddedPostgresDataSourceFactory() {
  }
}
