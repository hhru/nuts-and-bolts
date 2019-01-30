package ru.hh.nab.hibernate.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.hibernate.HibernateTestConfig;
import ru.hh.nab.testbase.hibernate.HibernateTestBase;
import static ru.hh.nab.datasource.DataSourceType.MASTER;
import static ru.hh.nab.datasource.DataSourceType.READONLY;
import static ru.hh.nab.datasource.DataSourceType.SLOW;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.clearMDC;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.executeOn;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.getDataSourceType;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.setDefaultMDC;

@ContextConfiguration(classes = {HibernateTestConfig.class})
public class DataSourceContextUnsafeTest extends HibernateTestBase {

  @Before
  public void setUp() {
    setDefaultMDC();
  }

  @Test
  public void testExecuteOn() {
    assertEquals(MASTER, getDataSourceType());
    assertEquals(MASTER, MDC.get(DataSourceContextUnsafe.MDC_KEY));

    executeOn(SLOW, () -> {
      assertEquals(SLOW, getDataSourceType());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));

      executeOn(READONLY, () -> {
        assertEquals(READONLY, getDataSourceType());
        assertEquals(READONLY, MDC.get(DataSourceContextUnsafe.MDC_KEY));
        return null;
      });

      assertEquals(SLOW, getDataSourceType());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });

    assertEquals(MASTER, getDataSourceType());
    assertEquals(MASTER, MDC.get(DataSourceContextUnsafe.MDC_KEY));
  }

  @Test
  public void testClearMDC() {
    assertEquals(MASTER, MDC.get(DataSourceContextUnsafe.MDC_KEY));

    clearMDC();

    assertNull(MDC.get(DataSourceContextUnsafe.MDC_KEY));
  }
}
