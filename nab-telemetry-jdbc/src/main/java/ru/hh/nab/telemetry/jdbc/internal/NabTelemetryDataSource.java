package ru.hh.nab.telemetry.jdbc.internal;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import static io.opentelemetry.instrumentation.jdbc.internal.JdbcUtils.computeDbInfo;
import io.opentelemetry.instrumentation.jdbc.internal.ThrowingSupplier;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import ru.hh.nab.datasource.DataSourcePropertiesStorage;
import ru.hh.nab.datasource.DelegatingDataSource;
import ru.hh.nab.datasource.NamedDataSource;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDataSourceInfo;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDbInfo;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDbRequest;

public class NabTelemetryDataSource extends DelegatingDataSource implements AutoCloseable {

  private final Instrumenter<NabDataSourceInfo, Void> dataSourceInstrumenter;
  private final Instrumenter<NabDbRequest, Void> statementInstrumenter;
  private final NabDataSourceInfo nabDataSourceInfo;

  public NabTelemetryDataSource(
      DataSource delegate,
      Instrumenter<NabDataSourceInfo, Void> connectionInstrumenter,
      Instrumenter<NabDbRequest, Void> statementInstrumenter
  ) {
    super(delegate);
    this.dataSourceInstrumenter = connectionInstrumenter;
    this.statementInstrumenter = statementInstrumenter;
    String dataSourceName = NamedDataSource.getName(delegate).orElse(null);
    this.nabDataSourceInfo = new NabDataSourceInfo()
        .setDataSource(delegate)
        .setDataSourceName(dataSourceName)
        .setWritableDataSource(dataSourceName == null ? null : DataSourcePropertiesStorage.isWritableDataSource(dataSourceName));
  }

  @Override
  public Connection getConnection() throws SQLException {
    Connection connection = wrapCall(delegate::getConnection);
    return new NabTelemetryConnection(connection, computeNabDbInfo(connection), statementInstrumenter);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    Connection connection = wrapCall(() -> delegate.getConnection(username, password));
    return new NabTelemetryConnection(connection, computeNabDbInfo(connection), statementInstrumenter);
  }

  @Override
  public void close() throws Exception {
    if (delegate instanceof AutoCloseable closable) {
      closable.close();
    }
  }

  private NabDbInfo computeNabDbInfo(Connection connection) {
    return new NabDbInfo().setDbInfo(computeDbInfo(connection)).setNabDataSourceInfo(nabDataSourceInfo);
  }

  private <T, E extends SQLException> T wrapCall(ThrowingSupplier<T, E> callable) throws E {
    Context parentContext = Context.current();

    if (!Span.fromContext(parentContext).getSpanContext().isValid()) {
      // this instrumentation is already very noisy, and calls to getConnection outside of an
      // existing trace do not tend to be very interesting
      return callable.call();
    }

    Context context = this.dataSourceInstrumenter.start(parentContext, nabDataSourceInfo);
    T result;
    try (Scope ignored = context.makeCurrent()) {
      result = callable.call();
    } catch (Throwable t) {
      this.dataSourceInstrumenter.end(context, nabDataSourceInfo, null, t);
      throw t;
    }
    this.dataSourceInstrumenter.end(context, nabDataSourceInfo, null, null);
    return result;
  }
}
