package ru.hh.nab.telemetry.jdbc.internal.getter;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.db.DbClientSpanNameExtractor;
import io.opentelemetry.instrumentation.jdbc.internal.DbRequest;
import io.opentelemetry.instrumentation.jdbc.internal.dbinfo.DbInfo;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import ru.hh.nab.telemetry.jdbc.internal.datasource.TestDataSource;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDataSourceInfo;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDbRequest;

public class ExtendedNameJdbcAttributesGetterTest {
  private static final SpanNameExtractor<NabDbRequest> nameExtractor = DbClientSpanNameExtractor.create(new ExtendedNameJdbcAttributesGetter());
  private static final DataSource dataSource = new TestDataSource(mock(DataSource.class));

  @Test
  public void testGetNameContainsNameIfSpecifiedInRequest() {
    DbClientSpanNameExtractor.create(new ExtendedNameJdbcAttributesGetter());

    var info = new NabDataSourceInfo().setDataSource(dataSource).setDataSourceName("readonly").setWritableDataSource(false);
    var nabRequest = new NabDbRequest().setNabDataSourceInfo(info).setDbRequest(createSelectHhVerificationDbRequest());
    String extracted = nameExtractor.extract(nabRequest);
    assertEquals("SELECT readonly hh.verification", extracted);
  }

  @Test
  public void testGetNameContainsDataSourceClassNameIfNameDoesntSpecifiedInRequest() {
    var info = new NabDataSourceInfo().setDataSource(dataSource);
    var nabRequest = new NabDbRequest().setNabDataSourceInfo(info).setDbRequest(createSelectHhVerificationDbRequest());
    String extracted = nameExtractor.extract(nabRequest);
    assertEquals("SELECT " + TestDataSource.class.getSimpleName() + " hh.verification", extracted);
  }

  private DbRequest createSelectHhVerificationDbRequest() {
    return DbRequest.create(DbInfo.builder().system("postgresql").db("hh").port(5432).build(), "SELECT * FROM verification LIMIT 10;");
  }
}
