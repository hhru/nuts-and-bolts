package ru.hh.nab.datasource;

import com.mchange.v2.beans.BeansUtils;
import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.DriverManagerDataSource;
import com.mchange.v2.c3p0.PoolBackedDataSource;
import com.mchange.v2.c3p0.WrapperConnectionPoolDataSource;
import com.mchange.v2.log.MLevel;
import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.datasource.monitoring.MonitoringDataSourceFactory;
import ru.hh.nab.datasource.monitoring.StatementTimeoutDataSource;

import javax.sql.DataSource;
import java.beans.IntrospectionException;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DataSourceFactory {
  private final MonitoringDataSourceFactory monitoringDataSourceFactory;

  DataSourceFactory(MonitoringDataSourceFactory monitoringDataSourceFactory) {
    this.monitoringDataSourceFactory = monitoringDataSourceFactory;
  }

  public DataSource create(DataSourceType dataSourceType, FileSettings settings) {
    String dataSourceName = dataSourceType.getName();
    return createDataSource(dataSourceName, settings.getSubSettings(dataSourceName));
  }

  private DataSource createDataSource(String dataSourceName, FileSettings dataSourceSettings) {
    DataSource underlyingDataSource = createDriverManagerDataSource(dataSourceName, dataSourceSettings);

    String statementTimeoutMsVal = dataSourceSettings.getString(DataSourceSettings.STATEMENT_TIMEOUT_MS);
    if (statementTimeoutMsVal != null) {
      int statementTimeoutMs = parseInt(statementTimeoutMsVal);
      if (statementTimeoutMs > 0) {
        underlyingDataSource = new StatementTimeoutDataSource(underlyingDataSource, statementTimeoutMs);
      }
    }

    underlyingDataSource = createC3P0DataSource(dataSourceName, underlyingDataSource, dataSourceSettings);

    checkDataSource(underlyingDataSource, dataSourceName);

    boolean sendStats = ofNullable(dataSourceSettings.getBoolean(DataSourceSettings.MONITORING_SEND_STATS)).orElse(false);
    return sendStats
        ? monitoringDataSourceFactory.create(dataSourceSettings, underlyingDataSource, dataSourceName)
        : underlyingDataSource;
  }

  private static DataSource createDriverManagerDataSource(String dataSourceName, FileSettings dataSourceSettings) {
    DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource(false);
    driverManagerDataSource.setJdbcUrl(dataSourceSettings.getString(DataSourceSettings.JDBC_URL));
    driverManagerDataSource.setUser(dataSourceSettings.getString(DataSourceSettings.USER));
    driverManagerDataSource.setPassword(dataSourceSettings.getString(DataSourceSettings.PASSWORD));
    driverManagerDataSource.setIdentityToken(dataSourceName);
    return driverManagerDataSource;
  }

  private static DataSource createC3P0DataSource(String dataSourceName, DataSource unpooledDataSource, FileSettings dataSourceSettings) {
    // We could use c3p0 DataSources.pooledDataSource(unpooledDataSource, c3p0Properties),
    // but it calls constructors with "autoregister=true" property.
    // This causes c3p0 to generate random identity token, registerDataSource datasource in jmx under this random token, which pollutes jmx metrics.
    // That is why we use constructors that allow to turn off "autoregister" property.

    try {
      Properties c3p0Properties = dataSourceSettings.getSubProperties(DataSourceSettings.C3P0_PREFIX);

      WrapperConnectionPoolDataSource wcpds = new WrapperConnectionPoolDataSource(false);
      wcpds.setNestedDataSource(unpooledDataSource);
      wcpds.setIdentityToken(dataSourceName);
      BeansUtils.overwriteAccessiblePropertiesFromMap(
          c3p0Properties,
          wcpds,
          false,
          null,
          true,
          MLevel.WARNING,
          MLevel.WARNING,
          true);

      PoolBackedDataSource poolBackedDataSource = new PoolBackedDataSource(false);
      poolBackedDataSource.setConnectionPoolDataSource(wcpds);
      poolBackedDataSource.setIdentityToken(dataSourceName);
      BeansUtils.overwriteAccessiblePropertiesFromMap(
          c3p0Properties,
          poolBackedDataSource,
          false,
          null,
          true,
          MLevel.WARNING,
          MLevel.WARNING,
          true);
      C3P0Registry.reregister(poolBackedDataSource.unwrap(PoolBackedDataSource.class));

      return poolBackedDataSource;

    } catch (IntrospectionException | SQLException | PropertyVetoException  e) {
      throw new RuntimeException("Failed to set " + dataSourceName + ".c3p0 properties: " + e.toString(), e);
    }
  }

  private static void checkDataSource(DataSource dataSource, String dataSourceName) {
    try (Connection connection = dataSource.getConnection()) {
      if (!connection.isValid(1000)) {
        throw new RuntimeException("Invalid connection to " + dataSourceName);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check data source " + dataSourceName + ": " + e.toString());
    }
  }
}
