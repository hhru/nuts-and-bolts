package ru.hh.nab.hibernate.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.context.ContextConfiguration;
import static ru.hh.nab.datasource.DataSourceType.MASTER;
import static ru.hh.nab.datasource.DataSourceType.READONLY;
import static ru.hh.nab.datasource.DataSourceType.SLOW;
import ru.hh.nab.hibernate.HibernateTestConfig;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.clearMDC;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.executeInScope;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.executeOn;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.getDataSourceKey;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.setDefaultMDC;
import ru.hh.nab.testbase.hibernate.HibernateTestBase;

@ContextConfiguration(classes = {HibernateTestConfig.class})
public class DataSourceContextUnsafeTest extends HibernateTestBase {

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
