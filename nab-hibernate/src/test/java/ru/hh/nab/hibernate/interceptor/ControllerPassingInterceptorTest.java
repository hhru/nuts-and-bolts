package ru.hh.nab.hibernate.interceptor;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import ru.hh.nab.common.mdc.MDC;

public class ControllerPassingInterceptorTest {
  private static final ControllerPassingInterceptor controllerPassingInterceptor = new ControllerPassingInterceptor();

  @AfterEach
  public void tearDown() {
    MDC.clearController();
  }

  @Test
  public void controllerExistShouldReturnWithComment() {
    MDC.setController("resume");

    String originalSql = "select * from resume;";

    String sqlAfterPrepareStatement = controllerPassingInterceptor.inspect(originalSql);

    assertEquals("/* resume */" + originalSql, sqlAfterPrepareStatement);
  }

  @Test
  public void controllerExistAndHasStarShouldReturnWithComment() {
    MDC.setController("resume*");

    String originalSql = "select * from resume;";

    String sqlAfterPrepareStatement = controllerPassingInterceptor.inspect(originalSql);

    assertEquals("/* resume_ */" + originalSql, sqlAfterPrepareStatement);
  }

  @Test
  public void controllerDoesNotExistShouldReturnWithoutComment() {
    assertFalse(MDC.getController().isPresent());

    String originalSql = "select * from resume;";

    String sqlAfterPrepareStatement = controllerPassingInterceptor.inspect(originalSql);

    assertEquals(originalSql, sqlAfterPrepareStatement);
  }
}
