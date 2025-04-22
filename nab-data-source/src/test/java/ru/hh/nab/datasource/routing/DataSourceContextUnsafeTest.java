package ru.hh.nab.datasource.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static ru.hh.nab.datasource.DataSourceType.MASTER;
import static ru.hh.nab.datasource.DataSourceType.READONLY;
import static ru.hh.nab.datasource.DataSourceType.SLOW;
import static ru.hh.nab.datasource.routing.DataSourceContextUnsafe.executeInScope;
import static ru.hh.nab.datasource.routing.DataSourceContextUnsafe.executeOn;
import static ru.hh.nab.datasource.routing.DataSourceContextUnsafe.getDataSourceName;

public class DataSourceContextUnsafeTest {

  @Test
  public void testExecuteOn() {
    assertEquals(MASTER, getDataSourceName());
    assertNull(MDC.get(DataSourceContextUnsafe.MDC_KEY));

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
