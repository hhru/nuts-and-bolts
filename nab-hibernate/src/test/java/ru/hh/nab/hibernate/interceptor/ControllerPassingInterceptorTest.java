package ru.hh.nab.hibernate.interceptor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import ru.hh.nab.common.mdc.MDC;

public class ControllerPassingInterceptorTest {
  private static final ControllerPassingInterceptor controllerPassingInterceptor = new ControllerPassingInterceptor();

  @After
  public void tearDown() {
    MDC.clearController();
  }

  @Test
  public void controllerExistShouldReturnWithComment() {
    MDC.setController("resume");

    String originalSql = "select * from resume;";

    String sqlAfterPrepareStatement = controllerPassingInterceptor.onPrepareStatement(originalSql);

    Assert.assertEquals("/* resume */" + originalSql, sqlAfterPrepareStatement);
  }

  @Test
  public void controllerExistAndHasStarShouldReturnWithComment() {
    MDC.setController("resume*");

    String originalSql = "select * from resume;";

    String sqlAfterPrepareStatement = controllerPassingInterceptor.onPrepareStatement(originalSql);

    Assert.assertEquals("/* resume_ */" + originalSql, sqlAfterPrepareStatement);
  }

  @Test
  public void controllerDoesNotExistShouldReturnWithoutComment() {
    Assert.assertFalse(MDC.getController().isPresent());

    String originalSql = "select * from resume;";

    String sqlAfterPrepareStatement = controllerPassingInterceptor.onPrepareStatement(originalSql);

    Assert.assertEquals(originalSql, sqlAfterPrepareStatement);
  }
}
