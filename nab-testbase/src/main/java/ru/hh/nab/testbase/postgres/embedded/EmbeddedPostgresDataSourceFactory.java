package ru.hh.nab.testbase.postgres.embedded;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import ru.hh.nab.common.files.FileSystemUtils;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.datasource.DataSourceFactory;
import ru.hh.nab.datasource.monitoring.MetricsTrackerFactoryProvider;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

import static ru.hh.nab.datasource.DataSourceSettings.JDBC_URL;
import static ru.hh.nab.datasource.DataSourceSettings.PASSWORD;
import static ru.hh.nab.datasource.DataSourceSettings.USER;

public class EmbeddedPostgresDataSourceFactory extends DataSourceFactory {
  public static final String DEFAULT_JDBC_URL = "jdbc:postgresql://localhost:%s/postgres";
  public static final String DEFAULT_USER = "postgres";

  private static final String PG_DIR = "embedded-pg";
  private static final String PG_DIR_PROPERTY = "ot.epg.working-dir";
  private static final UUID INSTANCE_ID = UUID.randomUUID();

  public EmbeddedPostgresDataSourceFactory() {
    super(null);
  }

  public EmbeddedPostgresDataSourceFactory(MetricsTrackerFactoryProvider metricsTrackerFactoryProvider) {
    super(metricsTrackerFactoryProvider);
  }

  protected DataSource createDataSource(String dataSourceName, boolean isReadonly, FileSettings settings) {
    Properties properties = settings.getProperties();
    properties.setProperty(JDBC_URL, String.format(DEFAULT_JDBC_URL, getEmbeddedPostgres().getPort()));
    properties.setProperty(USER, DEFAULT_USER);
    properties.setProperty(PASSWORD, "");
    return super.createDataSource(dataSourceName, isReadonly, new FileSettings(properties));
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
}
