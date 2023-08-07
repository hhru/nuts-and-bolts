package ru.hh.nab.datasource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.datasource.DataSourceSettings.DATASOURCE_NAME_FORMAT;

/**
 * Данный класс будет работать корректно только в том случае, если есть гарантия того, что все вызовы
 * метода {@link DatabaseSwitcher#getDataSourceName(String)} будут выполняться только после всех вызовов
 * метода {@link DatabaseSwitcher#createDataSourceName(String, String)}
 * (то есть если операции чтения выполняются только после всех операций записи).
 * <p>
 * Для того, чтобы приложение работало с несколькими базами данных, необходимо в service.properties добавить конфиги для всех датасорсов, используя
 * при этом имя базы данных в качестве префикса пропертей. В коде сервиса необходимо создать бин DatabaseSwitcher, заинжектив в него
 * databaseNameSupplier (в сапплаере можно, например, доставать значение какой-либо динамической настройки) + при создании датасорсов с помощью
 * фабричного метода {@link DataSourceFactory#create(String, String, boolean, FileSettings)} нужно передавать имя базы отдельным аргументом
 * (оно должно совпадать с префиксом в конфиге)
 * <p>
 * Пример конфига:
 * testDB.master.jdbcUrl=jdbc:postgresql://postgres:5432/testDB
 * testDB.master.pool.maximumPoolSize=10
 * testDB.readonly.jdbcUrl=jdbc:postgresql://postgres:5432/testDB
 * testDB.readonly.pool.maximumPoolSize=10
 *
 * prodDB.master.jdbcUrl=jdbc:postgresql://postgres:5432/prodDB
 * prodDB.master.pool.maximumPoolSize=10
 * prodDB.readonly.jdbcUrl=jdbc:postgresql://postgres:5432/prodDB
 * prodDB.readonly.pool.maximumPoolSize=10
 * <p>
 * Пример кода:
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
 * </p>
 */
public class DatabaseSwitcher {

  private final Supplier<String> databaseNameSupplier;
  private Map<String, Map<String, String>> dataSourceNamesForWrite = Map.of();
  private Map<String, Map<String, String>> dataSourceNamesForRead;

  public DatabaseSwitcher(Supplier<String> databaseNameSupplier) {
    this.databaseNameSupplier = databaseNameSupplier;
    DataSourceContextUnsafe.setDatabaseSwitcher(this);
  }

  public synchronized String createDataSourceName(String databaseName, String dataSourceType) {
    String dataSourceName = DATASOURCE_NAME_FORMAT.formatted(databaseName, dataSourceType);

    // convert immutable maps to mutable
    Map<String, Map<String, String>> dataSourceNames = dataSourceNamesForWrite
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashMap<>(entry.getValue()), (dsn1, dsn2) -> dsn1, HashMap::new));

    // put values to mutable maps
    dataSourceNames.computeIfAbsent(databaseName, i -> new HashMap<>()).put(dataSourceType, dataSourceName);

    // convert mutable maps to immutable
    dataSourceNamesForWrite = dataSourceNames
        .entrySet()
        .stream()
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));

    return dataSourceName;
  }

  public String getDataSourceName(String dataSourceType) {
    Map<String, Map<String, String>> dataSourceNames = getDataSourceNames();
    String databaseName = databaseNameSupplier.get();
    return Optional.ofNullable(dataSourceNames.get(databaseName))
        .map(dataSourceTypeToName -> dataSourceTypeToName.get(dataSourceType))
        .orElseThrow(() -> {
          String errorMessage = "DataSource with databaseName=%s, dataSourceType=%s is not found".formatted(databaseName, dataSourceType);
          return new IllegalArgumentException(errorMessage);
        });
  }

  private Map<String, Map<String, String>> getDataSourceNames() {
    if (dataSourceNamesForRead == null) {
      synchronized (this) {
        dataSourceNamesForRead = dataSourceNamesForWrite;
      }
    }
    return dataSourceNamesForRead;
  }
}
