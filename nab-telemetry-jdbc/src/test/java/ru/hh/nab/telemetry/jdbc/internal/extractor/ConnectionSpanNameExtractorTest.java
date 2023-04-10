package ru.hh.nab.telemetry.jdbc.internal.extractor;

import com.zaxxer.hikari.HikariDataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static ru.hh.nab.telemetry.jdbc.internal.extractor.ConnectionSpanNameExtractor.GET_CONNECTION;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDataSourceInfo;

public class ConnectionSpanNameExtractorTest {

  private static final ConnectionSpanNameExtractor connectionSpanNameExtractor = new ConnectionSpanNameExtractor();

  @Test
  public void testExtractContainsNameIfSpecifiedInInfo() {
    var info = new NabDataSourceInfo().setDataSource(new HikariDataSource()).setDataSourceName("readonly").setWritableDataSource(false);
    String extracted = connectionSpanNameExtractor.extract(info);
    assertEquals(GET_CONNECTION + " readonly", extracted);
  }

  @Test
  public void testExtractContainsDataSourceClassNameIfNameDoesntSpecifiedInInfo() {
    var info = new NabDataSourceInfo().setDataSource(new HikariDataSource());
    String extracted = connectionSpanNameExtractor.extract(info);
    assertEquals(GET_CONNECTION + " HikariDataSource", extracted);
  }
}
