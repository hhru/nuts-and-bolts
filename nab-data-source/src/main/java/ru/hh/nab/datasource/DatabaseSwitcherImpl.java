package ru.hh.nab.datasource;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.datasource.DataSourceSettings.DATASOURCE_NAME_FORMAT;
import ru.hh.nab.jdbc.common.DatabaseSwitcher;

/**
 * Для того, чтобы приложение работало с несколькими базами данных, необходимо в service.properties добавить конфиги для всех датасорсов, используя
 * при этом имя базы данных в качестве префикса пропертей. В коде сервиса необходимо создать бин DatabaseSwitcher с помощью конструктора
 * {@link DatabaseSwitcherImpl#DatabaseSwitcherImpl(Supplier)} (в сапплаере можно, например, доставать значение какой-либо динамической настройки) +
 * при создании датасорсов с помощью фабричного метода {@link DataSourceFactory#create(String, String, boolean, FileSettings)} нужно передавать имя
 * базы отдельным аргументом (оно должно совпадать с префиксом в конфиге)
 * <p>
 * Пример конфига:
 * <pre>{@code
 * testDB.master.jdbcUrl=jdbc:postgresql://postgres:5432/testDB
 * testDB.master.pool.maximumPoolSize=10
 * testDB.readonly.jdbcUrl=jdbc:postgresql://postgres:5432/testDB
 * testDB.readonly.pool.maximumPoolSize=10
 *
 * prodDB.master.jdbcUrl=jdbc:postgresql://postgres:5432/prodDB
 * prodDB.master.pool.maximumPoolSize=10
 * prodDB.readonly.jdbcUrl=jdbc:postgresql://postgres:5432/prodDB
 * prodDB.readonly.pool.maximumPoolSize=10
 * }</pre>
 * <p>
 * Пример кода:
 * <pre>{@code
 * private static final String TARGET_DATABASE_SETTING = "target_database";
 * private static final String TEST_DB = "testDB";
 * private static final String PROD_DB = "prodDB";
 *
 * @Bean
 * RoutingDataSource dataSource(
 *     DataSourceFactory dataSourceFactory,
 *     RoutingDataSourceFactory routingDataSourceFactory,
 *     FileSettings fileSettings
 * ) {
 *   var routingDataSource = routingDataSourceFactory.create();
 *   routingDataSource.addNamedDataSource(dataSourceFactory.create(TEST_DB, DataSourceType.MASTER, false, fileSettings));
 *   routingDataSource.addNamedDataSource(dataSourceFactory.create(TEST_DB, DataSourceType.READONLY, true, fileSettings));
 *   routingDataSource.addNamedDataSource(dataSourceFactory.create(PROD_DB, DataSourceType.MASTER, false, fileSettings));
 *   routingDataSource.addNamedDataSource(dataSourceFactory.create(PROD_DB, DataSourceType.READONLY, true, fileSettings));
 *   return routingDataSource;
 * }
 *
 * @Bean
 * DatabaseSwitcher databaseSwitcher(SettingsClient settingsClient) {
 *   return new DatabaseSwitcher(
 *       () -> settingsClient.getString(TARGET_DATABASE_SETTING)
 *           .orElseThrow(() -> new IllegalStateException("Setting %s is not configured".formatted(TARGET_DATABASE_SETTING)))
 *   );
 * }
 * }</pre>
 * </p>
 */
public class DatabaseSwitcherImpl implements DatabaseSwitcher {

  private final Supplier<String> databaseNameSupplier;
  private final Map<String, Map<String, String>> dataSourceNames = new HashMap<>();

  public DatabaseSwitcherImpl(Supplier<String> databaseNameSupplier) {
    this.databaseNameSupplier = databaseNameSupplier;
  }

  @PostConstruct
  public void configureDataSourceContextUnsafe() {
    DataSourceContextUnsafe.setDatabaseSwitcher(this);
  }

  @Override
  public String createDataSourceName(String databaseName, String dataSourceType) {
    String dataSourceName = DATASOURCE_NAME_FORMAT.formatted(databaseName, dataSourceType);
    dataSourceNames.computeIfAbsent(databaseName, k -> new HashMap<>()).put(dataSourceType, dataSourceName);
    return dataSourceName;
  }

  @Override
  public String getDataSourceName(String dataSourceType) {
    String databaseName = databaseNameSupplier.get();
    return Optional
        .ofNullable(dataSourceNames.get(databaseName))
        .map(dataSourceTypeToName -> dataSourceTypeToName.get(dataSourceType))
        .orElseThrow(() -> {
          String errorMessage = "DataSource with databaseName=%s, dataSourceType=%s is not found".formatted(databaseName, dataSourceType);
          return new IllegalArgumentException(errorMessage);
        });
  }
}
