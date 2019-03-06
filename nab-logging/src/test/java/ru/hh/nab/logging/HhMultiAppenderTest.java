package ru.hh.nab.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Test;
import ru.hh.nab.logging.json.NabJsonLayout;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class HhMultiAppenderTest {

  @Test
  public void testIfLogToConsoleSetConsoleAppenderCreated() {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.toConsole", "true");
    context.putProperty("log.syslogHost", "localhost");
    context.putProperty("log.syslogPort", "123");
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.start();
    assertThat(multiAppender.appender, is(instanceOf(ConsoleAppender.class)));
    checkIfAppenderProperlyConfigured(multiAppender.appender);
  }

  @Test
  public void testSyslogHostAndPortSetSyslogAppenderCreated() {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.syslogHost", "localhost");
    context.putProperty("log.syslogPort", "123");
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.start();
    assertThat(multiAppender.appender, is(instanceOf(HhSyslogAppender.class)));
    checkIfAppenderProperlyConfigured(multiAppender.appender);
  }

  @Test
  public void testIfSyslogPortOnlySetRollingAppenderCreated() {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.syslogPort", "123");
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.start();
    assertThat(multiAppender.appender, is(instanceOf(HhRollingAppender.class)));
    checkIfAppenderProperlyConfigured(multiAppender.appender);
  }

  @Test
  public void testIfSyslogPortSetWrongRollingAppenderCreated() {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.syslogHost", "localhost");
    context.putProperty("log.syslogPort", "abc");
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.start();
    assertThat(multiAppender.appender, is(instanceOf(HhRollingAppender.class)));
    checkIfAppenderProperlyConfigured(multiAppender.appender);
  }

  @Test
  public void testIfJsonUnSetPatternStructuredLayoutSet() throws Exception {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.start();
    checkIfAppenderProperlyConfigured(multiAppender.appender);
    ThrowableSupplier layoutSupplier = createLayoutSupplier(multiAppender.appender);
    assertThat(layoutSupplier.get(), is(instanceOf(PatternLayout.class)));
  }

  @Test
  public void testIfPatternInAppenderOverridesContext() throws Exception {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    String pattern = "%msg%n";
    multiAppender.setPattern(pattern);
    multiAppender.start();
    checkIfAppenderProperlyConfigured(multiAppender.appender);
    ThrowableSupplier layoutSupplier = createLayoutSupplier(multiAppender.appender);
    PatternLayout layout = (PatternLayout) layoutSupplier.get();
    assertEquals(pattern, layout.getPattern());
  }

  @Test(expected = AssertionError.class)
  public void testIfNoPatternAvailableExceptionIsThrown() {
    LoggerContext context = new LoggerContext();
    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.start();
  }

  @Test
  public void testIfJsonSetJsonStructuredLayoutSet() throws Exception {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.syslogHost", "localhost");
    context.putProperty("log.syslogPort", "123");
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.setJson(true);
    multiAppender.start();
    checkIfAppenderProperlyConfigured(multiAppender.appender);
    ThrowableSupplier layoutSupplier = createLayoutSupplier(multiAppender.appender);
    assertThat(layoutSupplier.get(), is(instanceOf(NabJsonLayout.class)));
  }

  private static void checkIfAppenderProperlyConfigured(Appender<?> appender) {
    assertNotNull("context is not set", appender.getContext());
    assertTrue("appender is not started", appender.isStarted());
    ThrowableSupplier layoutSupplier = createLayoutSupplier(appender);
    if (layoutSupplier != null) {
      try {
        assertNotNull("layout is not set", layoutSupplier.get());
      } catch (Exception ignored) {
      }
    }
  }

  private static ThrowableSupplier createLayoutSupplier(Appender<?> appender) {
    ThrowableSupplier layoutSupplier = null;
    try {
      Method getLayout = appender.getClass().getMethod("getLayout");
      layoutSupplier = () -> getLayout.invoke(appender);
    } catch (NoSuchMethodException e) {
      try {
        Method getEncoder = appender.getClass().getMethod("getEncoder");
        Object encoder = getEncoder.invoke(appender);
        if (encoder instanceof LayoutWrappingEncoder) {
          Method getLayoutFromEncoder = LayoutWrappingEncoder.class.getMethod("getLayout");
          layoutSupplier = () -> getLayoutFromEncoder.invoke(encoder);
        }
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
      }
    }
    return layoutSupplier;
  }

  protected static HhMultiAppender createHhMultiAppender(LoggerContext context) {
    HhMultiAppender multiAppender = new HhMultiAppender();
    multiAppender.setName("test");
    multiAppender.setContext(context);
    return multiAppender;
  }

  @FunctionalInterface
  private interface ThrowableSupplier {
    Object get() throws Exception;
  }
}
