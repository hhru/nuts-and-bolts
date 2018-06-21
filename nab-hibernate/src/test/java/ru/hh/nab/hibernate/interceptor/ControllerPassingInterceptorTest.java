package ru.hh.nab.hibernate.interceptor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import ru.hh.nab.common.util.MDC;

public class ControllerPassingInterceptorTest {
  private static final ControllerPassingInterceptor controllerPassingInterceptor = new ControllerPassingInterceptor();

  @After
  public void tearDown() {
    MDC.deleteKey(MDC.CONTROLLER_MDC_KEY);
  }

  @Test
  public void controllerExistShouldReturnWithComment() throws Exception {
    MDC.setKey(MDC.CONTROLLER_MDC_KEY, "resume");

    String originalSql = "select * from resume;";

    String sqlAfterPrepareStatement = controllerPassingInterceptor.onPrepareStatement(originalSql);

    Assert.assertEquals("/* resume */" + originalSql, sqlAfterPrepareStatement);
  }

  @Test
  public void controllerExistAndHasStarShouldReturnWithComment() throws Exception {
    MDC.setKey(MDC.CONTROLLER_MDC_KEY, "resume*");

    String originalSql = "select * from resume;";

    String sqlAfterPrepareStatement = controllerPassingInterceptor.onPrepareStatement(originalSql);

    Assert.assertEquals("/* resume_ */" + originalSql, sqlAfterPrepareStatement);
  }

  @Test
  public void controllerDoesNotExistShouldReturnWithoutComment() throws Exception {
    Assert.assertFalse(MDC.getController().isPresent());

    String originalSql = "select * from resume;";

    String sqlAfterPrepareStatement = controllerPassingInterceptor.onPrepareStatement(originalSql);

    Assert.assertEquals(originalSql, sqlAfterPrepareStatement);
  }
}
