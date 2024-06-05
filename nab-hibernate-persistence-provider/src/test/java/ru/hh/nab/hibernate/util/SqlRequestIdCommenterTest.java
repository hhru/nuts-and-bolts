package ru.hh.nab.hibernate.util;

import org.apache.commons.lang3.RandomStringUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import ru.hh.nab.common.mdc.MDC;

public class SqlRequestIdCommenterTest {
  @Test
  public void testSqlRequestIds() {
    String SQL = "SELECT * FROM resume;";

    MDC.runWithRequestId("Valid-rid_0123", () -> {
      assertEquals("/* Valid-rid_0123 */ SELECT * FROM resume;", SqlRequestIdCommenter.addRequestIdComment(SQL));
      return null;
    });

    String rid = RandomStringUtils.randomAlphanumeric(100);
    MDC.runWithRequestId(rid, () -> {
      assertEquals(String.format("/* %s */ SELECT * FROM resume;", rid), SqlRequestIdCommenter.addRequestIdComment(SQL));
      return null;
    });

    MDC.runWithRequestId(RandomStringUtils.randomAlphanumeric(101), () -> {
      assertEquals("SELECT * FROM resume;", SqlRequestIdCommenter.addRequestIdComment(SQL));
      return null;
    });

    MDC.runWithRequestId("*/ DELETE FROM resume; /*", () -> {
      assertEquals("SELECT * FROM resume;", SqlRequestIdCommenter.addRequestIdComment(SQL));
      return null;
    });
  }
}
