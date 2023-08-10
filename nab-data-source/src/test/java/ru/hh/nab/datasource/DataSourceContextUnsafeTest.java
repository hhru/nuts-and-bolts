package ru.hh.nab.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static ru.hh.nab.datasource.DataSourceContextUnsafe.clearMDC;
import static ru.hh.nab.datasource.DataSourceContextUnsafe.executeInScope;
import static ru.hh.nab.datasource.DataSourceContextUnsafe.executeOn;
import static ru.hh.nab.datasource.DataSourceContextUnsafe.getDataSourceKey;
import static ru.hh.nab.datasource.DataSourceContextUnsafe.setDefaultMDC;
import static ru.hh.nab.datasource.DataSourceType.MASTER;
import static ru.hh.nab.datasource.DataSourceType.READONLY;
import static ru.hh.nab.datasource.DataSourceType.SLOW;

public class DataSourceContextUnsafeTest {

  @BeforeEach
  public void setUp() {
    setDefaultMDC();
  }

  @Test
  public void testExecuteOn() {
    assertEquals(MASTER, getDataSourceKey());
    assertEquals(MASTER, MDC.get(DataSourceContextUnsafe.MDC_KEY));

    executeOn(SLOW, false, () -> {
      assertEquals(SLOW, getDataSourceKey());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));

      executeOn(READONLY, false, () -> {
        assertEquals(READONLY, getDataSourceKey());
        assertEquals(READONLY, MDC.get(DataSourceContextUnsafe.MDC_KEY));
        return null;
      });

      assertEquals(SLOW, getDataSourceKey());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });

    assertEquals(MASTER, getDataSourceKey());
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
    DataSourceContextUnsafe.setRequestScopeDataSourceKey("test");
    executeInScope("test", () -> executeOn(SLOW, false, () -> {
      assertEquals(SLOW, getDataSourceKey());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));

      executeOn(READONLY, false, () -> {
        assertEquals(READONLY, getDataSourceKey());
        assertEquals(READONLY, MDC.get(DataSourceContextUnsafe.MDC_KEY));
        return null;
      });

      assertEquals(SLOW, getDataSourceKey());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    }));
  }

  @Test
  public void testRequestScopeSetOverrideEnabled() {
    var testKey = "test";
    executeInScope(testKey, () -> executeOn(SLOW, true, () -> {
      assertEquals(testKey, getDataSourceKey());
      assertEquals(testKey, MDC.get(DataSourceContextUnsafe.MDC_KEY));

      executeOn(READONLY, true, () -> {
        assertEquals(testKey, getDataSourceKey());
        assertEquals(testKey, MDC.get(DataSourceContextUnsafe.MDC_KEY));
        return null;
      });

      assertEquals(testKey, getDataSourceKey());
      assertEquals(testKey, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    }));
  }

  @Test
  public void testRequestScopeUnsetOverrideEnabled() {
    executeOn(SLOW, true, () -> {
      assertEquals(SLOW, getDataSourceKey());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));

      executeOn(READONLY, true, () -> {
        assertEquals(READONLY, getDataSourceKey());
        assertEquals(READONLY, MDC.get(DataSourceContextUnsafe.MDC_KEY));
        return null;
      });

      assertEquals(SLOW, getDataSourceKey());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });
  }
}
