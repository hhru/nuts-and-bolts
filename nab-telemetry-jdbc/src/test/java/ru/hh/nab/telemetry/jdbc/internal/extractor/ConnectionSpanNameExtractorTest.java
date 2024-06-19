package ru.hh.nab.telemetry.jdbc.internal.extractor;

import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import ru.hh.nab.telemetry.jdbc.internal.datasource.TestDataSource;
import static ru.hh.nab.telemetry.jdbc.internal.extractor.ConnectionSpanNameExtractor.GET_CONNECTION;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDataSourceInfo;

public class ConnectionSpanNameExtractorTest {

  private static final ConnectionSpanNameExtractor connectionSpanNameExtractor = new ConnectionSpanNameExtractor();
  private static final DataSource dataSource = new TestDataSource(mock(DataSource.class));

  @Test
  public void testExtractContainsNameIfSpecifiedInInfo() {
    var info = new NabDataSourceInfo().setDataSource(dataSource).setDataSourceName("readonly").setWritableDataSource(false);
    String extracted = connectionSpanNameExtractor.extract(info);
    assertEquals(GET_CONNECTION + " readonly", extracted);
  }

  @Test
  public void testExtractContainsDataSourceClassNameIfNameDoesntSpecifiedInInfo() {
    var info = new NabDataSourceInfo().setDataSource(dataSource);
    String extracted = connectionSpanNameExtractor.extract(info);
    assertEquals(GET_CONNECTION + " " + TestDataSource.class.getSimpleName(), extracted);
  }
}
