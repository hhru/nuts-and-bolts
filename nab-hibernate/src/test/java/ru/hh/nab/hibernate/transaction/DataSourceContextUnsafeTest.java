package ru.hh.nab.hibernate.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import ru.hh.nab.hibernate.HibernateTestBase;
import static ru.hh.nab.datasource.DataSourceType.MASTER;
import static ru.hh.nab.datasource.DataSourceType.READONLY;
import static ru.hh.nab.datasource.DataSourceType.SLOW;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.clearMDC;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.executeOn;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.getDataSourceType;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.setDefaultMDC;

public class DataSourceContextUnsafeTest extends HibernateTestBase {

  @Before
  public void setUp() {
    setDefaultMDC();
  }

  @Test
  public void testExecuteOn() {
    assertNull(getDataSourceType());
    assertEquals(MASTER.getName(), MDC.get(DataSourceContextUnsafe.MDC_KEY));

    executeOn(SLOW, () -> {
      assertEquals(SLOW, getDataSourceType());
      assertEquals(SLOW.getName(), MDC.get(DataSourceContextUnsafe.MDC_KEY));

      executeOn(READONLY, () -> {
        assertEquals(READONLY, getDataSourceType());
        assertEquals(READONLY.getName(), MDC.get(DataSourceContextUnsafe.MDC_KEY));
        return null;
      });

      assertEquals(SLOW, getDataSourceType());
      assertEquals(SLOW.getName(), MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });

    assertNull(getDataSourceType());
    assertEquals(MASTER.getName(), MDC.get(DataSourceContextUnsafe.MDC_KEY));
  }

  @Test
  public void testClearMDC() {
    assertEquals(MASTER.getName(), MDC.get(DataSourceContextUnsafe.MDC_KEY));

    clearMDC();

    assertNull(MDC.get(DataSourceContextUnsafe.MDC_KEY));
  }
}
