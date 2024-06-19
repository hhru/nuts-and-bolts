package ru.hh.nab.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static ru.hh.nab.datasource.DataSourceContextUnsafe.clearMDC;
import static ru.hh.nab.datasource.DataSourceContextUnsafe.executeInScope;
import static ru.hh.nab.datasource.DataSourceContextUnsafe.executeOn;
import static ru.hh.nab.datasource.DataSourceContextUnsafe.getDataSourceName;
import static ru.hh.nab.datasource.DataSourceContextUnsafe.setDefaultMDC;
import static ru.hh.nab.jdbc.common.DataSourceType.MASTER;
import static ru.hh.nab.jdbc.common.DataSourceType.READONLY;
import static ru.hh.nab.jdbc.common.DataSourceType.SLOW;

public class DataSourceContextUnsafeTest {

  @BeforeEach
  public void setUp() {
    setDefaultMDC();
  }

  @Test
  public void testExecuteOn() {
    assertEquals(MASTER, getDataSourceName());
    assertEquals(MASTER, MDC.get(DataSourceContextUnsafe.MDC_KEY));

    executeOn(SLOW, false, () -> {
      assertEquals(SLOW, getDataSourceName());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));

      executeOn(READONLY, false, () -> {
        assertEquals(READONLY, getDataSourceName());
        assertEquals(READONLY, MDC.get(DataSourceContextUnsafe.MDC_KEY));
        return null;
      });

      assertEquals(SLOW, getDataSourceName());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });

    assertEquals(MASTER, getDataSourceName());
    assertEquals(MASTER, MDC.get(DataSourceContextUnsafe.MDC_KEY));
  }

  @Test
  public void testClearMDC() {
    assertEquals(MASTER, MDC.get(DataSourceContextUnsafe.MDC_KEY));

    clearMDC();

    assertNull(MDC.get(DataSourceContextUnsafe.MDC_KEY));
  }

  @Test
  public void testRequestScopeSetOverrideDisabled() {
    DataSourceContextUnsafe.setRequestScopeDataSourceType("test");
    executeInScope("test", () -> executeOn(SLOW, false, () -> {
      assertEquals(SLOW, getDataSourceName());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));

      executeOn(READONLY, false, () -> {
        assertEquals(READONLY, getDataSourceName());
        assertEquals(READONLY, MDC.get(DataSourceContextUnsafe.MDC_KEY));
        return null;
      });

      assertEquals(SLOW, getDataSourceName());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    }));
  }

  @Test
  public void testRequestScopeSetOverrideEnabled() {
    var testKey = "test";
    executeInScope(testKey, () -> executeOn(SLOW, true, () -> {
      assertEquals(testKey, getDataSourceName());
      assertEquals(testKey, MDC.get(DataSourceContextUnsafe.MDC_KEY));

      executeOn(READONLY, true, () -> {
        assertEquals(testKey, getDataSourceName());
        assertEquals(testKey, MDC.get(DataSourceContextUnsafe.MDC_KEY));
        return null;
      });

      assertEquals(testKey, getDataSourceName());
      assertEquals(testKey, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    }));
  }

  @Test
  public void testRequestScopeUnsetOverrideEnabled() {
    executeOn(SLOW, true, () -> {
      assertEquals(SLOW, getDataSourceName());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));

      executeOn(READONLY, true, () -> {
        assertEquals(READONLY, getDataSourceName());
        assertEquals(READONLY, MDC.get(DataSourceContextUnsafe.MDC_KEY));
        return null;
      });

      assertEquals(SLOW, getDataSourceName());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });
  }
}
