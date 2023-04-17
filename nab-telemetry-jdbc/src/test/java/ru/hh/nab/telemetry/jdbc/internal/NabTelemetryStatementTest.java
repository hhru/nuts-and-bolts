package ru.hh.nab.telemetry.jdbc.internal;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.jdbc.internal.dbinfo.DbInfo;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDataSourceInfo;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDbInfo;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDbRequest;

@ExtendWith(MockitoExtension.class)
public class NabTelemetryStatementTest {

  @Mock
  private Instrumenter<NabDbRequest, Void> instrumenter;

  private NabTelemetryStatement<Statement> statement;

  @BeforeEach
  public void setUp() {
    NabDbInfo nabDbInfo = new NabDbInfo().setDbInfo(mock(DbInfo.class)).setNabDataSourceInfo(mock(NabDataSourceInfo.class));
    statement = new NabTelemetryStatement<>(mock(Statement.class), nabDbInfo, instrumenter);
  }

  @Test
  public void testSelectQueryNotSkipped() throws SQLException {
    Context context = mock(Context.class);
    doReturn(true).when(instrumenter).shouldStart(any(), any());
    doReturn(context).when(instrumenter).start(any(), any());
    statement.executeQuery("Select * from vacancies limit 10");
    verify(instrumenter).start(any(), any());
  }

  @Test
  public void testSetTimeoutQuerySkipped() throws SQLException {
    statement.executeQuery("Set Statement_timeout To ?");
    verify(instrumenter, never()).start(any(), any());
  }
}
